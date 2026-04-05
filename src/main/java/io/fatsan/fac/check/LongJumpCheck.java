package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementContextSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects long-jump cheats that traverse an abnormally large horizontal distance
 * in a single movement event.
 *
 * <p><b>Cheat behaviour:</b> "Long jump" modules give the player a large
 * horizontal velocity impulse on the jump frame, producing a single-tick delta
 * far exceeding the vanilla maximum.  Vanilla max horizontal single-tick delta
 * is approximately 0.6 blocks (sprint-jump with Speed II: ~0.55–0.58 blocks).
 * Long jump cheats commonly produce 1.5–6+ block deltas in one tick.
 *
 * <p><b>Signal:</b> {@link MovementContextSignal#deltaXZ()} is the horizontal
 * distance moved this tick.  The threshold of {@value #THRESHOLD_BLOCKS} blocks
 * per tick is set above the vanilla speed-boost maximum.
 *
 * <p><b>False-positive mitigations:</b>
 * <ul>
 *   <li>Slime block launches can produce 1.0+ block deltas — threshold at 1.5.</li>
 *   <li>Interval guard: skip if interval > 200ms (lag spike produces large delta).</li>
 *   <li>Buffer of 2: require two consecutive events.</li>
 * </ul>
 */
public final class LongJumpCheck extends AbstractBufferedCheck {

  private static final double THRESHOLD_BLOCKS = 1.5;
  private static final long INTERVAL_GUARD_NS = 200_000_000L;

  public LongJumpCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "LongJump";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementContextSignal ctx)) {
      return CheckResult.clean(name(), category());
    }

    // Lag spike guard: large interval means accumulated delta — skip
    if (ctx.intervalNanos() > INTERVAL_GUARD_NS) {
      return CheckResult.clean(name(), category());
    }

    // In vehicle or water: vanilla mechanics may exceed normal limits
    if (ctx.inVehicle() || ctx.inWater() || ctx.inLava() || ctx.gliding()) {
      coolDown(ctx.playerId());
      return CheckResult.clean(name(), category());
    }

    if (ctx.deltaXZ() > THRESHOLD_BLOCKS) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Long jump: deltaXZ=" + String.format("%.3f", ctx.deltaXZ())
                + " blocks buf=" + buf,
            Math.min(1.0, ctx.deltaXZ() / 4.0),
            true);
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
