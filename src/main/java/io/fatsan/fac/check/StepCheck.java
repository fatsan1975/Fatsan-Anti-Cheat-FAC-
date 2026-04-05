package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects Step hack — clients that override the vanilla step height limit,
 * allowing players to step over blocks taller than 0.6 blocks without jumping.
 *
 * <p>Vanilla step height is 0.625 blocks.  A step hack typically allows 1.0
 * or 2.0 block steps, letting the player walk through staircases or over
 * walls as if they were flat ground while reporting {@code onGround=true}.
 *
 * <p>Detection: {@code onGround=true} + {@code deltaY} between 0.625 and 1.6
 * + significant horizontal movement.  The upper bound excludes legitimate
 * jump landings, and the horizontal threshold excludes standing still near
 * a block edge.
 */
public final class StepCheck extends AbstractBufferedCheck {

  /** Vanilla maximum step height (blocks). Anything above is suspicious. */
  private static final double MAX_VANILLA_STEP = 0.625D;

  /** Upper bound to avoid flagging normal jump arcs that land with onGround. */
  private static final double MAX_STEP_HEIGHT = 1.65D;

  /** Minimum horizontal speed (bps) — eliminates edge-pushing near walls. */
  private static final double MIN_LATERAL_BPS = 1.5D;

  private static final double MAX_INTERVAL_SECONDS = 0.15D;

  public StepCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "Step";
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
      return CheckResult.clean(name(), category());
    }

    if (!movement.onGround()) {
      coolDown(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    double seconds = movement.intervalNanos() / 1_000_000_000.0D;
    if (seconds <= 0.0D || seconds > MAX_INTERVAL_SECONDS) {
      return CheckResult.clean(name(), category());
    }

    double speedBps = movement.deltaXZ() / seconds;

    if (movement.deltaY() > MAX_VANILLA_STEP
        && movement.deltaY() <= MAX_STEP_HEIGHT
        && speedBps > MIN_LATERAL_BPS) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(),
            "Step height exceeded (deltaY=" + String.format("%.3f", movement.deltaY())
                + " speed=" + String.format("%.1f", speedBps) + "bps)",
            Math.min(1.0D, buf / 4.0D),
            true);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
