package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects sustained flight — remaining airborne while gaining or maintaining
 * upward/horizontal velocity beyond vanilla gravity limits.
 *
 * <p><b>Cheat behaviour:</b> "Flight" cheats cancel gravity so {@code deltaY}
 * never decreases at the expected 0.08 blocks/tick rate.  The player stays at
 * the same Y or moves upward while {@code onGround == false} and no elytra,
 * vehicle, or ladder is active.
 *
 * <p><b>Detection:</b> We track consecutive ticks where
 * {@code deltaY >= -0.05} (gravity-near-zero or upward) while not on ground,
 * not gliding, not in vehicle, not climbable, and not in water/lava.
 * A streak of {@value #STREAK_THRESHOLD} consecutive such ticks triggers.
 *
 * <p><b>False-positive mitigations:</b>
 * <ul>
 *   <li>Jump apex has deltaY ≈ 0; exempt by requiring ≥ 5 consecutive ticks.</li>
 *   <li>Gliding, vehicle, climbable, water, lava all bypass the check.</li>
 * </ul>
 *
 * <p><b>Folia:</b> PlayerStateEvent is immutable, emitted on region thread.
 */
public final class FlightSustainedCheck extends AbstractBufferedCheck {

  private static final int STREAK_THRESHOLD = 5;
  private static final double GRAVITY_ZERO_THRESHOLD = -0.05;

  /** Per-player count of consecutive suspicious airborne ticks. */
  private final Map<String, Integer> streaks = new ConcurrentHashMap<>();

  public FlightSustainedCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "FlightSustained";
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

    // Skip all legitimate airborne states
    if (state.onGround() || state.gliding() || state.inVehicle()
        || state.climbable() || state.inWater() || state.inLava()) {
      streaks.remove(state.playerId());
      coolDown(state.playerId());
      return CheckResult.clean(name(), category());
    }

    if (state.deltaY() >= GRAVITY_ZERO_THRESHOLD) {
      int streak = streaks.merge(state.playerId(), 1, Integer::sum);
      if (streak >= STREAK_THRESHOLD) {
        int buf = incrementBuffer(state.playerId());
        if (overLimit(buf)) {
          return new CheckResult(
              true,
              name(),
              category(),
              "Sustained flight: deltaY=" + String.format("%.3f", state.deltaY())
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
