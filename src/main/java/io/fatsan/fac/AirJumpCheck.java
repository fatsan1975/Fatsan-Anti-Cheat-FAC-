package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects double-jump / air-jump cheats — gaining upward velocity while already
 * in the air without a legitimate source (elytra, vehicle, climbable, fluid).
 *
 * <p><b>Cheat behaviour:</b> "Double jump" and "air jump" modules allow a player
 * to jump again mid-air by injecting an upward velocity impulse when they press
 * the jump key while already airborne.  In vanilla, pressing jump in the air has
 * no effect on vertical velocity.
 *
 * <p><b>Detection:</b> We track the previous {@code onGround} and {@code deltaY}
 * state.  An air jump is detected when:
 * <ul>
 *   <li>Previous tick: {@code !onGround AND deltaY < -0.05} (falling or
 *       descending)</li>
 *   <li>Current tick: {@code !onGround AND deltaY > 0.25} (positive vertical
 *       acceleration without landing)</li>
 * </ul>
 * This transition — falling then suddenly moving upward without touching ground
 * — is impossible in vanilla physics.
 *
 * <p><b>Folia thread safety:</b> per-player state in {@code ConcurrentHashMap}.
 */
public final class AirJumpCheck extends AbstractBufferedCheck {

  private static final double FALLING_THRESHOLD = -0.05;
  private static final double JUMP_THRESHOLD = 0.25;

  private final Map<String, Boolean> prevOnGround = new ConcurrentHashMap<>();
  private final Map<String, Double> prevDeltaY = new ConcurrentHashMap<>();

  public AirJumpCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "AirJump";
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

    // Exempt legitimate airborne sources
    if (state.gliding() || state.inVehicle() || state.climbable()
        || state.inWater() || state.inLava()) {
      prevOnGround.put(state.playerId(), state.onGround());
      prevDeltaY.put(state.playerId(), state.deltaY());
      return CheckResult.clean(name(), category());
    }

    boolean wasOnGround = prevOnGround.getOrDefault(state.playerId(), true);
    double lastDeltaY = prevDeltaY.getOrDefault(state.playerId(), 0.0);

    prevOnGround.put(state.playerId(), state.onGround());
    prevDeltaY.put(state.playerId(), state.deltaY());

    // Detect: was falling in air → now jumping in air (without landing)
    if (!wasOnGround && !state.onGround()
        && lastDeltaY < FALLING_THRESHOLD
        && state.deltaY() > JUMP_THRESHOLD) {
      int buf = incrementBuffer(state.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Air jump: prevDeltaY=" + String.format("%.3f", lastDeltaY)
                + " curDeltaY=" + String.format("%.3f", state.deltaY())
                + " buf=" + buf,
            Math.min(1.0, buf / 4.0),
            true);
      }
    } else {
      coolDown(state.playerId());
    }
    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    prevOnGround.remove(playerId);
    prevDeltaY.remove(playerId);
  }
}
