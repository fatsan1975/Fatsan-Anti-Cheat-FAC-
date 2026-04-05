package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects elytra flight acceleration without firework rockets.
 *
 * <p><b>Cheat behaviour:</b> Elytra cheats that provide unlimited boost allow
 * sustained horizontal flight speed far above what vanilla elytra provides
 * without fireworks (max ~30 bps glide, ~60 bps with firework boost).
 * Without a firework, elytra glide speed decays; "elytra boost" cheats maintain
 * or increase it.
 *
 * <p><b>Detection:</b> While gliding ({@code gliding == true}), horizontal
 * speed should naturally decay.  We track horizontal speed across a window;
 * if speed consistently exceeds the vanilla non-firework cap of
 * {@value #VANILLA_GLIDE_MAX_BPS} bps for {@value #STREAK_THRESHOLD} ticks,
 * we flag.
 *
 * <p><b>Note:</b> This check cannot distinguish a firework-boosted flight from
 * a cheat-boosted one without packet data.  It is conservative: the firework
 * boost cap is ~60 bps (vanilla documented value), so we only flag above that.
 *
 * <p>Uses {@link io.fatsan.fac.model.MovementContextSignal} fields; we use
 * {@code PlayerStateEvent.deltaXZ} and interval to estimate bps here for
 * simplicity, accepting ~10% imprecision.
 */
public final class ElytraNoFireworkCheck extends AbstractBufferedCheck {

  /** Max glide speed without a firework rocket (blocks per second). */
  private static final double VANILLA_GLIDE_MAX_BPS = 30.0;
  /** Max speed even with firework boost — above this is impossible vanilla. */
  private static final double FIREWORK_MAX_BPS = 65.0;

  private static final int STREAK_THRESHOLD = 4;
  private static final long INTERVAL_GUARD_NS = 200_000_000L;

  private final Map<String, Integer> fastGlideStreak = new ConcurrentHashMap<>();

  public ElytraNoFireworkCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "ElytraNoFirework";
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

    if (!state.gliding()) {
      fastGlideStreak.remove(state.playerId());
      coolDown(state.playerId());
      return CheckResult.clean(name(), category());
    }

    // Estimate horizontal speed — guard against lag spikes
    if (state.intervalNanos() <= 0 || state.intervalNanos() > INTERVAL_GUARD_NS) {
      return CheckResult.clean(name(), category());
    }

    double seconds = state.intervalNanos() / 1e9;
    double bps = state.deltaXZ() / seconds;

    if (bps > FIREWORK_MAX_BPS) {
      // Clearly above any vanilla possibility
      int buf = incrementBuffer(state.playerId());
      fastGlideStreak.merge(state.playerId(), 1, Integer::sum);
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Elytra speed above firework max: " + String.format("%.1f", bps) + " bps buf=" + buf,
            Math.min(1.0, buf / 4.0),
            true);
      }
    } else if (bps > VANILLA_GLIDE_MAX_BPS) {
      // Could be firework boost — accumulate cautiously
      int streak = fastGlideStreak.merge(state.playerId(), 1, Integer::sum);
      if (streak >= STREAK_THRESHOLD) {
        int buf = incrementBuffer(state.playerId());
        if (overLimit(buf)) {
          return new CheckResult(
              true,
              name(),
              category(),
              "Sustained elytra speed without firework: " + String.format("%.1f", bps)
                  + " bps streak=" + streak + " buf=" + buf,
              Math.min(0.75, buf / 6.0), // lower confidence — firework might exist
              false);
        }
      }
    } else {
      fastGlideStreak.remove(state.playerId());
      coolDown(state.playerId());
    }
    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    fastGlideStreak.remove(playerId);
  }
}
