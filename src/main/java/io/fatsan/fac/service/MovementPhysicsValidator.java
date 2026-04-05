package io.fatsan.fac.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified Minecraft physics engine for server-side movement prediction.
 *
 * <p>Tracks per-player velocity state and predicts where the player should be
 * based on vanilla physics rules.  Checks compare the predicted delta against
 * the actual observed delta to detect flight, speed, and other movement hacks.
 *
 * <p>Precision note: This is a simplified model.  Bukkit events do not fire
 * every tick (players may send multiple position packets before Bukkit fires
 * an event), so predictions include a configurable tolerance.  The goal is to
 * catch obvious deviations, not pixel-perfect validation.
 *
 * <p>Physics constants (all per-tick unless noted):
 * <ul>
 *   <li>Gravity: dy -= 0.08 per tick, then dy *= 0.98</li>
 *   <li>Air horizontal drag: dxz *= 0.91 per tick</li>
 *   <li>Ground friction: dxz *= (slipperiness * 0.91) — default block 0.546</li>
 *   <li>Sprint walk speed: ~0.2806 blocks/tick on flat ground</li>
 *   <li>Normal walk speed: ~0.2171 blocks/tick on flat ground</li>
 * </ul>
 */
public final class MovementPhysicsValidator {

  /** Gravity constant applied per tick. */
  public static final double GRAVITY = 0.08D;

  /** Vertical drag multiplier applied per tick while airborne. */
  public static final double VERTICAL_DRAG = 0.98D;

  /** Horizontal air drag multiplier per tick. */
  public static final double AIR_HORIZONTAL_DRAG = 0.91D;

  /** Ground friction (default block slipperiness * air drag). */
  public static final double GROUND_FRICTION = 0.546D;

  /** Maximum sprint speed (blocks/tick) — slightly above vanilla for tolerance. */
  public static final double MAX_SPRINT_SPEED = 0.30D;

  /** Maximum sprint speed in blocks/second (at 20tps). */
  public static final double MAX_SPRINT_BPS = MAX_SPRINT_SPEED * 20.0D;

  /**
   * Maximum upward velocity per tick from a vanilla jump
   * (0.42 + 0.1 * jumpBoostLevel, base = 0.42).
   */
  public static final double MAX_JUMP_VELOCITY = 0.42D;

  /** Tolerance factor applied to predictions to account for timing imprecision. */
  private static final double TOLERANCE_FACTOR = 1.25D;

  private final Map<String, PhysicsState> states = new ConcurrentHashMap<>();

  /** Returns the current physics state for the player, or a fresh state if none. */
  public PhysicsState getState(String playerId) {
    return states.getOrDefault(playerId, PhysicsState.INITIAL);
  }

  /**
   * Updates the physics state based on observed movement.
   *
   * @param playerId  the player
   * @param deltaXZ   observed horizontal displacement
   * @param deltaY    observed vertical displacement
   * @param onGround  whether the player was reported on ground
   * @param intervalNanos  time since last event in nanoseconds
   * @return the new state after the update
   */
  public PhysicsState update(
      String playerId,
      double deltaXZ,
      double deltaY,
      boolean onGround,
      long intervalNanos) {
    PhysicsState prev = states.getOrDefault(playerId, PhysicsState.INITIAL);
    double ticks = intervalNanos / 50_000_000.0; // 1 tick = 50ms
    ticks = Math.max(0.5, Math.min(ticks, 4.0)); // clamp to [0.5, 4] ticks

    double nextVy;
    if (onGround) {
      nextVy = 0.0;
    } else {
      // Apply gravity per tick
      nextVy = (prev.velocityY() - GRAVITY) * VERTICAL_DRAG;
    }

    double nextVxz;
    if (onGround) {
      nextVxz = deltaXZ * GROUND_FRICTION / ticks;
    } else {
      nextVxz = deltaXZ * AIR_HORIZONTAL_DRAG / ticks;
    }

    PhysicsState next = new PhysicsState(nextVxz, nextVy, onGround, System.nanoTime());
    states.put(playerId, next);
    return next;
  }

  /**
   * Predicts the expected maximum horizontal speed for a player in blocks/second,
   * accounting for potion effects and sprint state.
   *
   * @param sprinting     true if the player is sprinting
   * @param speedLevel    speed potion amplifier (-1 = none, 0 = Speed I, 1 = Speed II)
   * @param slowLevel     slowness potion amplifier (-1 = none, 0 = Slow I)
   * @return expected max horizontal speed in bps
   */
  public static double predictMaxHorizontalBps(boolean sprinting, int speedLevel, int slowLevel) {
    double base = sprinting ? MAX_SPRINT_BPS : (MAX_SPRINT_BPS * 0.75);
    if (speedLevel >= 0) base *= (1.0 + 0.2 * (speedLevel + 1));
    if (slowLevel >= 0) base *= (1.0 - 0.15 * (slowLevel + 1));
    return base * TOLERANCE_FACTOR;
  }

  /**
   * Returns true if the observed deltaY is impossible for a non-cheating player
   * given the previous physics state.
   *
   * @param prevState     the state before the current event
   * @param observedDeltaY  the observed vertical displacement
   * @param onGround      whether the player is on ground
   * @param gliding       whether the player is gliding (elytra)
   * @return true if the vertical motion is physically implausible
   */
  public static boolean isImpossibleVertical(
      PhysicsState prevState,
      double observedDeltaY,
      boolean onGround,
      boolean gliding) {
    if (gliding || onGround) return false;
    // Maximum upward velocity from a jump
    double maxUp = MAX_JUMP_VELOCITY * TOLERANCE_FACTOR;
    // Maximum downward velocity is theoretically unbounded but practically capped
    if (observedDeltaY > maxUp && prevState.velocityY() < observedDeltaY * 0.5) {
      return true;
    }
    return false;
  }

  /** Clears all state for the given player. Called on disconnect. */
  public void clearPlayer(String playerId) {
    states.remove(playerId);
  }

  public record PhysicsState(
      double velocityXZ,
      double velocityY,
      boolean wasOnGround,
      long timestamp) {

    public static final PhysicsState INITIAL = new PhysicsState(0.0, 0.0, true, 0L);
  }
}
