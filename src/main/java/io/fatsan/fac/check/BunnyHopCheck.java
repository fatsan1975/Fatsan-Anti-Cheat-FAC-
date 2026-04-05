package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects bunny-hop / auto-jump cheats that repeatedly jump at the optimal tick.
 *
 * <p><b>Cheat behaviour:</b> "Bunny hop" cheats re-jump the exact tick the player
 * lands, exploiting the 30% speed boost the game grants on the jump frame.
 * Because vanilla input is limited by human reaction time (~80–200ms), a player
 * landing and instantly re-jumping every single time at inhuman precision is
 * characteristic of a macro or cheat module.
 *
 * <p><b>Detection:</b> We watch for the transition {@code onGround == true →
 * deltaY > 0} (jump) and measure the ground-contact interval.  If a player
 * lands and re-jumps within {@value #INSTANT_GROUND_NS} nanoseconds consistently
 * across {@value #PATTERN_WINDOW} consecutive hops, we flag.
 *
 * <p><b>State machine:</b>
 * <pre>
 *   wasOnGround=true + deltaY > 0  → record groundContactNanos as jump event
 *   wasOnGround=true + deltaY ≤ 0  → update groundLandNanos
 * </pre>
 *
 * <p><b>False-positive mitigations:</b>
 * <ul>
 *   <li>Water, lava, climbable exempt (bunny is impossible there).</li>
 *   <li>Require 4 consecutive instant hops before flagging.</li>
 * </ul>
 */
public final class BunnyHopCheck extends AbstractBufferedCheck {

  /** If a player re-jumps within this time of landing, it is considered "instant". */
  private static final long INSTANT_GROUND_NS = 60_000_000L; // 60ms
  private static final int PATTERN_WINDOW = 4;

  private final Map<String, Long> landNanos = new ConcurrentHashMap<>();
  private final Map<String, Boolean> wasOnGround = new ConcurrentHashMap<>();
  private final Map<String, Integer> instantHops = new ConcurrentHashMap<>();

  public BunnyHopCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "BunnyHop";
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

    if (state.inWater() || state.inLava() || state.climbable()
        || state.gliding() || state.inVehicle()) {
      resetPlayer(state.playerId());
      return CheckResult.clean(name(), category());
    }

    boolean prevOnGround = wasOnGround.getOrDefault(state.playerId(), false);
    wasOnGround.put(state.playerId(), state.onGround());

    if (prevOnGround && state.deltaY() > 0.3) {
      // Jump event — player was on ground last tick and now has upward velocity
      long land = landNanos.getOrDefault(state.playerId(), 0L);
      long groundContact = state.nanoTime() - land;

      if (land > 0 && groundContact < INSTANT_GROUND_NS) {
        int hops = instantHops.merge(state.playerId(), 1, Integer::sum);
        if (hops >= PATTERN_WINDOW) {
          int buf = incrementBuffer(state.playerId());
          if (overLimit(buf)) {
            return new CheckResult(
                true,
                name(),
                category(),
                "BunnyHop: " + hops + " instant re-jumps (contact="
                    + (groundContact / 1_000_000) + "ms buf=" + buf + ")",
                Math.min(1.0, buf / 5.0),
                true);
          }
        }
      } else {
        instantHops.remove(state.playerId());
        coolDown(state.playerId());
      }
    } else if (state.onGround() && state.deltaY() <= 0) {
      // Landing tick
      landNanos.put(state.playerId(), state.nanoTime());
    }

    return CheckResult.clean(name(), category());
  }

  private void resetPlayer(String playerId) {
    landNanos.remove(playerId);
    wasOnGround.remove(playerId);
    instantHops.remove(playerId);
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    resetPlayer(playerId);
  }
}
