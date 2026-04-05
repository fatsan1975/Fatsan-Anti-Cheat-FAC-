package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects Spider / WallClimb — climbing vertical surfaces that are not
 * ladders, vines, or other climbable blocks.
 *
 * <p>In vanilla Minecraft, upward movement while airborne follows a jump
 * arc: velocity decreases each tick due to gravity.  Spider hack bypasses
 * this by providing constant upward velocity against a wall without the
 * player being on a climbable block.
 *
 * <p>Detection: sustained small positive deltaY while airborne (not on
 * ground, not gliding, not in vehicle) with very low coefficient of
 * variation — gravity should make legitimate jump arcs non-uniform.
 * The horizontal speed is also low because spider is primarily vertical.
 */
public final class SpiderCheck extends AbstractWindowCheck {

  /** Minimum mean deltaY (blocks/event) to consider suspicious. */
  private static final double MIN_SPIDER_DELTA_Y = 0.08D;

  /** Maximum mean deltaY — above this it's a jump or other movement. */
  private static final double MAX_SPIDER_DELTA_Y = 0.25D;

  /**
   * Maximum horizontal speed (bps) — spider movement is mostly vertical.
   * Above this the player might just be on a slope.
   */
  private static final double MAX_LATERAL_BPS = 3.5D;

  private static final double MAX_INTERVAL_SECONDS = 0.15D;

  public SpiderCheck(int limit) {
    super(limit, 8);
  }

  @Override
  public String name() {
    return "Spider";
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
      coolDown(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    double seconds = movement.intervalNanos() / 1_000_000_000.0D;
    if (seconds <= 0.0D || seconds > MAX_INTERVAL_SECONDS) {
      return CheckResult.clean(name(), category());
    }

    // Only track events with small upward deltaY in the suspicious range
    if (movement.deltaY() <= 0.0D) {
      coolDown(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    double speedBps = movement.deltaXZ() / seconds;
    if (speedBps > MAX_LATERAL_BPS) {
      return CheckResult.clean(name(), category());
    }

    var ws = stats.record(movement.playerId(), movement.deltaY());
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    // Uniform small upward movement while airborne = wall climbing
    boolean steadyAscent = ws.mean() >= MIN_SPIDER_DELTA_Y
        && ws.mean() <= MAX_SPIDER_DELTA_Y
        && ws.isUniformlyCadenced(0.15);

    if (steadyAscent) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(),
            "Wall climb (meanDeltaY=" + String.format("%.3f", ws.mean())
                + " cv=" + String.format("%.3f", ws.entropyScore()) + ")",
            Math.min(1.0D, buf / 5.0D),
            false);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
