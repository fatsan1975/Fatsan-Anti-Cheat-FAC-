package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects sustained anomalous airborne horizontal acceleration — consistent
 * with speed-hack or air-strafe clients that bypass the vanilla air-resistance
 * envelope while airborne.
 *
 * <p>Legitimate players lose horizontal momentum while airborne due to the
 * vanilla air-drag constant (0.91× per tick).  A client that maintains or
 * rapidly increases horizontal speed while airborne exceeds what physics allows.
 *
 * <p>Uses {@link AbstractWindowCheck} to track a window of recent air speeds.
 * A spike is flagged when the current speed is significantly above the window
 * mean (established baseline), preventing single-outlier false positives while
 * catching sustained speed manipulation.  The window is cleared on landing so
 * the baseline resets for each airborne phase.
 */
public final class AirStrafeAccelerationCheck extends AbstractWindowCheck {

  /** Minimum speed (blocks/sec) below which acceleration spikes are not checked. */
  private static final double MIN_SUSPICIOUS_SPEED_BPS = 12.0D;

  /** Minimum speed delta above the window mean to constitute a spike. */
  private static final double MIN_SPIKE_DELTA_BPS = 6.0D;

  /** Maximum event interval (seconds) considered valid for speed computation. */
  private static final double MAX_INTERVAL_SECONDS = 0.15D;

  public AirStrafeAccelerationCheck(int limit) {
    super(limit, 6);
  }

  @Override
  public String name() {
    return "AirStrafeAcceleration";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement) || movement.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    if (movement.onGround() || movement.gliding() || movement.inVehicle()) {
      // Reset the window on landing so each airborne phase starts fresh.
      stats.clear(movement.playerId());
      coolDown(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    double seconds = movement.intervalNanos() / 1_000_000_000.0D;
    if (seconds <= 0.0D || seconds > MAX_INTERVAL_SECONDS) {
      return CheckResult.clean(name(), category());
    }

    double speedBps = movement.deltaXZ() / seconds;

    // Capture the window mean before adding the current observation so we
    // compare against the established baseline rather than the updated mean.
    var prev = stats.peek(movement.playerId());
    stats.record(movement.playerId(), speedBps);

    boolean spike = speedBps > MIN_SUSPICIOUS_SPEED_BPS
        && prev.hasEnoughData()
        && (speedBps - prev.mean()) > MIN_SPIKE_DELTA_BPS;

    if (spike) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Airborne acceleration spike above baseline (speed="
                + String.format("%.1f", speedBps)
                + "bps baseline="
                + String.format("%.1f", prev.mean())
                + "bps)",
            Math.min(1.0D, buf / 8.0D),
            true);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
