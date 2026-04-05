package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;

/**
 * Detects SafeWalk: cheat that automatically sneaks at block edges to
 * prevent falling. Normal sneak speed is ~1.3 bps = ~0.065 blocks/tick.
 * SafeWalk players sustain abnormally high speed while sneaking.
 */
public final class SafeWalkEdgeCheck extends AbstractBufferedCheck {
  private static final double MAX_SNEAK_SPEED_PER_TICK = 0.13;

  public SafeWalkEdgeCheck(int limit) { super(limit); }

  @Override public String name() { return "SafeWalkEdge"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof PlayerStateEvent state)) return CheckResult.clean(name(), category());

    if (!state.sneaking() || !state.onGround()) {
      coolDown(state.playerId());
      return CheckResult.clean(name(), category());
    }
    if (state.inVehicle() || state.gliding() || state.climbable()) return CheckResult.clean(name(), category());

    if (state.deltaXZ() > MAX_SNEAK_SPEED_PER_TICK) {
      int buf = incrementBuffer(state.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(),
            "SafeWalk pattern (sneakSpeed=" + String.format("%.3f", state.deltaXZ()) + " limit=" + MAX_SNEAK_SPEED_PER_TICK + ")",
            Math.min(1.0, buf / 6.0), true);
      }
    } else {
      coolDown(state.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
