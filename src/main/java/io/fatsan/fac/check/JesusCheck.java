package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;

/**
 * Detects Jesus (water/lava walking) — the cheat that allows players to walk
 * on the surface of water or lava as if it were solid ground.
 *
 * <p>Vanilla players in water/lava sink at a rate affected by depth-strider
 * and other effects, but they cannot maintain sprint-level horizontal speed
 * while their Y position stays flat (deltaY ≈ 0) inside the fluid block.
 *
 * <p>Detection strategy: if the player is inside a fluid (water or lava),
 * is not gliding or in a vehicle, and maintains a high horizontal speed with
 * near-zero vertical displacement, this is consistent with walking on the
 * fluid surface rather than swimming through it.
 */
public final class JesusCheck extends AbstractWindowCheck {

  /**
   * Minimum horizontal speed (blocks/second) in fluid to be suspicious.
   * Vanilla swimming is much slower; this threshold allows for Depth Strider III.
   */
  private static final double MIN_SUSPICIOUS_FLUID_SPEED_BPS = 4.8D;

  /** Maximum absolute deltaY to consider the player "not sinking". */
  private static final double MAX_SURFACE_DELTA_Y = 0.05D;

  /** Maximum interval seconds accepted for speed computation. */
  private static final double MAX_INTERVAL_SECONDS = 0.15D;

  public JesusCheck(int limit) {
    super(limit, 6);
  }

  @Override
  public String name() {
    return "Jesus";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof PlayerStateEvent state)) {
      return CheckResult.clean(name(), category());
    }

    if (!state.inWater() && !state.inLava()) {
      coolDown(state.playerId());
      return CheckResult.clean(name(), category());
    }

    if (state.inVehicle() || state.gliding() || state.climbable()) {
      return CheckResult.clean(name(), category());
    }

    double seconds = state.intervalNanos() / 1_000_000_000.0D;
    if (seconds <= 0.0D || seconds > MAX_INTERVAL_SECONDS) {
      return CheckResult.clean(name(), category());
    }

    double speedBps = state.deltaXZ() / seconds;
    boolean notSinking = Math.abs(state.deltaY()) <= MAX_SURFACE_DELTA_Y;

    var ws = stats.record(state.playerId(), speedBps);
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() > MIN_SUSPICIOUS_FLUID_SPEED_BPS && notSinking) {
      int buf = incrementBuffer(state.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "High speed in "
                + (state.inLava() ? "lava" : "water")
                + " with no vertical sink (mean="
                + String.format("%.1f", ws.mean())
                + "bps deltaY="
                + String.format("%.3f", state.deltaY())
                + ")",
            Math.min(1.0D, buf / 6.0D),
            false);
      }
    } else {
      coolDown(state.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
