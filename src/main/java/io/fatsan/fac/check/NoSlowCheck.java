package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;

/**
 * Detects NoSlow — the cheat that allows players to maintain full sprint speed
 * while using items (eating food, drinking potions, drawing a bow, or blocking
 * with a shield).
 *
 * <p>In vanilla Minecraft, using an item applies a 50 % horizontal speed
 * penalty.  A player sprinting at full speed while eating or blocking exceeds
 * what the server should allow.
 *
 * <p>Thresholds are intentionally conservative to avoid false positives from
 * speed effects, ViaVersion timing, or block surface variance.
 */
public final class NoSlowCheck extends AbstractWindowCheck {

  /**
   * Maximum horizontal speed (blocks/second) considered acceptable while
   * using an item.  Vanilla slow-use walk speed is ~3.2 bps; this threshold
   * includes generous tolerance for lag, Speed I potion, and speed effects.
   */
  private static final double MAX_USE_ITEM_SPEED_BPS = 6.5D;

  /** Maximum interval seconds accepted for speed computation. */
  private static final double MAX_INTERVAL_SECONDS = 0.15D;

  public NoSlowCheck(int limit) {
    super(limit, 8);
  }

  @Override
  public String name() {
    return "NoSlow";
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

    if (!state.eating() && !state.blocking()) {
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

    var ws = stats.record(state.playerId(), speedBps);
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() > MAX_USE_ITEM_SPEED_BPS) {
      int buf = incrementBuffer(state.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Full speed while using item (mean="
                + String.format("%.1f", ws.mean())
                + "bps using="
                + (state.eating() ? "eat" : "block")
                + ")",
            Math.min(1.0D, buf / 6.0D),
            true);
      }
    } else {
      coolDown(state.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
