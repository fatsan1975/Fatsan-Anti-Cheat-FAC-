package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects hovering — staying in the air with deltaY ≈ 0 without an elytra.
 *
 * <p><b>Cheat behaviour:</b> "Hover" cheats keep the player stationary in the
 * air or moving only horizontally.  Unlike {@code FlightSustainedCheck} which
 * targets upward movement, this check targets the case where deltaY is very
 * close to zero (the player is neither falling nor climbing) for many consecutive
 * ticks without any legitimate explanation.
 *
 * <p><b>Detection:</b> A player hovering has {@code |deltaY| <= 0.01} while
 * {@code onGround == false} and no elytra/vehicle/water/lava/climbable.
 * Vanilla physics make this impossible beyond the single frame at the apex
 * of a jump (where deltaY briefly equals 0).
 *
 * <p>Requires {@value #STREAK_THRESHOLD} consecutive hover ticks before
 * flagging (the apex tick is always 1, so ≥ 3 strongly implies a cheat).
 */
public final class HoveringCheck extends AbstractBufferedCheck {

  private static final int STREAK_THRESHOLD = 3;
  private static final double HOVER_DELTA_MAX = 0.01;

  private final Map<String, Integer> streaks = new ConcurrentHashMap<>();

  public HoveringCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "Hovering";
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

    if (state.onGround() || state.gliding() || state.inVehicle()
        || state.climbable() || state.inWater() || state.inLava()) {
      streaks.remove(state.playerId());
      coolDown(state.playerId());
      return CheckResult.clean(name(), category());
    }

    if (Math.abs(state.deltaY()) <= HOVER_DELTA_MAX) {
      int streak = streaks.merge(state.playerId(), 1, Integer::sum);
      if (streak >= STREAK_THRESHOLD) {
        int buf = incrementBuffer(state.playerId());
        if (overLimit(buf)) {
          return new CheckResult(
              true,
              name(),
              category(),
              "Hover: deltaY=" + String.format("%.4f", state.deltaY())
                  + " streak=" + streak + " buf=" + buf,
              Math.min(1.0, buf / 5.0),
              true);
        }
      }
    } else {
      streaks.remove(state.playerId());
      coolDown(state.playerId());
    }
    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    streaks.remove(playerId);
  }
}
