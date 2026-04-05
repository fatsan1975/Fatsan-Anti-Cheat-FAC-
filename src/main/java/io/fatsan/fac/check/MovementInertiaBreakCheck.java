package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects abrupt airborne inertia breaks — consistent with fly-hack or
 * no-slowdown clients that can instantly stop horizontal momentum mid-air,
 * bypassing the vanilla air-drag envelope.
 *
 * <p>Vanilla physics applies a constant 0.91× drag per tick while airborne.
 * A sudden stop from sustained horizontal movement while the player is still
 * airborne is physically impossible under vanilla, making it a reliable
 * signal when it recurs.
 *
 * <p>Uses {@link AbstractWindowCheck} to track a window of recent deltaXZ
 * values.  The window mean serves as the established movement baseline.
 * An abrupt stop is flagged when the current delta drops far below the mean
 * while the player is airborne, which is more robust than comparing only two
 * consecutive values (the old approach could be bypassed with periodic
 * outlier frames).
 */
public final class MovementInertiaBreakCheck extends AbstractWindowCheck {

  /** Minimum window mean deltaXZ that constitutes "sustained movement". */
  private static final double MIN_BASELINE_DELTA = 0.18D;

  /** Maximum deltaXZ considered an abrupt stop. */
  private static final double MAX_STOP_DELTA = 0.02D;

  /** Minimum drop below the window mean to constitute an inertia break. */
  private static final double MIN_JERK_BELOW_MEAN = 0.16D;

  public MovementInertiaBreakCheck(int limit) {
    super(limit, 6);
  }

  @Override
  public String name() {
    return "MovementInertiaBreak";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) {
      return CheckResult.clean(name(), category());
    }
    if (movement.gliding() || movement.inVehicle() || movement.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    // Capture the baseline mean before recording the current value.
    var prev = stats.peek(movement.playerId());
    stats.record(movement.playerId(), movement.deltaXZ());

    boolean abruptStop = !movement.onGround()
        && prev.hasEnoughData()
        && prev.mean() > MIN_BASELINE_DELTA
        && movement.deltaXZ() < MAX_STOP_DELTA
        && (movement.deltaXZ() - prev.mean()) < -MIN_JERK_BELOW_MEAN;

    if (abruptStop) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Abrupt airborne inertia break (delta="
                + String.format("%.3f", movement.deltaXZ())
                + " baseline="
                + String.format("%.3f", prev.mean())
                + ")",
            Math.min(1.0D, buf / 7.0D),
            false);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
