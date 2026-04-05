package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects Phase — the cheat that allows players to move through solid blocks.
 *
 * <p>Phase is detected heuristically: if a player has significant horizontal
 * displacement but reports being on the ground with an impossible combination
 * of vertical and horizontal motion (moving fast while the server thinks they
 * are grounded and not moving vertically), the pattern is consistent with
 * collider bypass.
 *
 * <p>More precise phase detection requires server-side block collision
 * simulation (not yet implemented).  This check focuses on detectable
 * anomalies in the reported ground/position state that are hard to fake
 * consistently without leaving a statistical trace.
 *
 * <p>Specifically, this check flags when a player:
 * <ul>
 *   <li>Reports {@code onGround=true}</li>
 *   <li>Has significant lateral displacement</li>
 *   <li>Has a large positive deltaY (impossible while on ground)</li>
 * </ul>
 * This combination occurs when a client claims to be on the ground while
 * simultaneously reporting upward displacement — a common artefact of
 * block-phase exploits where the client is passing through the floor.
 */
public final class PhaseCheck extends AbstractBufferedCheck {

  /**
   * Minimum horizontal speed (blocks/second) to consider checking.
   * Low-speed movement near block edges is common and not suspicious.
   */
  private static final double MIN_LATERAL_BPS = 2.0D;

  /**
   * Maximum positive deltaY that is still consistent with "on ground".
   * Stepping up a block while on ground can cause small positive deltaY (up to 0.625).
   * We allow a little more to avoid false positives from block step-ups.
   */
  private static final double MAX_GROUND_DELTA_Y = 0.8D;

  /** Maximum interval seconds accepted for speed computation. */
  private static final double MAX_INTERVAL_SECONDS = 0.15D;

  public PhaseCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "Phase";
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

    if (movement.gliding() || movement.inVehicle()) {
      coolDown(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    double seconds = movement.intervalNanos() / 1_000_000_000.0D;
    if (seconds <= 0.0D || seconds > MAX_INTERVAL_SECONDS) {
      return CheckResult.clean(name(), category());
    }

    double speedBps = movement.deltaXZ() / seconds;

    // Detect ground+upward anomaly: client claims onGround but deltaY is too high
    boolean groundUpwardAnomaly = movement.onGround()
        && movement.deltaY() > MAX_GROUND_DELTA_Y
        && speedBps > MIN_LATERAL_BPS;

    if (groundUpwardAnomaly) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Ground+upward phase anomaly (deltaY="
                + String.format("%.3f", movement.deltaY())
                + " speed="
                + String.format("%.1f", speedBps)
                + "bps onGround=true)",
            Math.min(1.0D, buf / 5.0D),
            false);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
