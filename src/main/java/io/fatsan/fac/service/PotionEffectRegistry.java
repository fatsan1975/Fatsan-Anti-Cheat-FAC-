package io.fatsan.fac.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of active potion effects per player, used by movement
 * and combat checks to correctly allow potion-modified behaviour.
 *
 * <h2>Why this is necessary on large servers</h2>
 *
 * <p>Many movement and combat checks have hard thresholds calibrated for vanilla
 * player capabilities.  However, vanilla potion effects legitimately modify these
 * limits:
 *
 * <ul>
 *   <li><b>Speed I/II:</b> Increases horizontal movement speed by 20%/40%.
 *       Without awareness, {@code SpeedEnvelopeCheck} and {@code DashHackCheck}
 *       flag every Speed II player.</li>
 *   <li><b>Jump Boost I/II:</b> Increases jump height and horizontal launch
 *       velocity.  {@code LongJumpCheck} must allow larger deltas.</li>
 *   <li><b>Haste I/II:</b> Increases block break speed by 20%/40%.
 *       {@code FastBreakCheck} and {@code InstantBreakHardrockCheck} must
 *       reduce their minimum break time accordingly.</li>
 *   <li><b>Slow Falling:</b> Reduces fall speed to ~0.01 blocks/tick.
 *       {@code HoveringCheck} would fire on every Slow Falling player.</li>
 *   <li><b>Levitation:</b> Causes upward movement without elytra.
 *       {@code FlightSustainedCheck} must allow upward deltaY.</li>
 *   <li><b>Dolphin's Grace:</b> Increases swimming speed to ~5 bps.
 *       {@code LiquidAccelerationCheck} must raise the water threshold.</li>
 * </ul>
 *
 * <h2>Data model</h2>
 *
 * <p>Per-player effects are stored as a map of {@code effect name → amplifier}
 * (0-indexed: Haste I = 0, Haste II = 1).  Effects are set on
 * {@code EntityPotionEffectEvent} and cleared on effect expiry or player quit.
 *
 * <h2>Thread safety</h2>
 *
 * <p>All access is through {@code ConcurrentHashMap}.  The inner effect map per
 * player is replaced atomically.  Reads and writes from any Folia region thread
 * are safe.
 */
public final class PotionEffectRegistry {

  /**
   * Known potion effect names used by checks.
   *
   * <p>These are Bukkit {@code PotionEffectType.getKey().getKey()} values
   * (lowercase, no namespace).
   */
  public static final String SPEED = "speed";
  public static final String JUMP_BOOST = "jump_boost";
  public static final String HASTE = "haste";
  public static final String SLOW_FALLING = "slow_falling";
  public static final String LEVITATION = "levitation";
  public static final String DOLPHINS_GRACE = "dolphins_grace";
  public static final String STRENGTH = "strength";
  public static final String WEAKNESS = "weakness";
  public static final String SLOW = "slowness";

  /** Per-player active effects: effectKey → amplifier (0-indexed). */
  private final Map<String, Map<String, Integer>> playerEffects = new ConcurrentHashMap<>();

  /**
   * Records that a player gained or had an existing potion effect refreshed.
   *
   * @param playerId   UUID string
   * @param effectKey  lowercase Bukkit effect key (e.g. "speed", "haste")
   * @param amplifier  0-indexed amplifier level (0 = level I, 1 = level II)
   */
  public void onEffectAdd(String playerId, String effectKey, int amplifier) {
    playerEffects
        .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
        .put(effectKey, amplifier);
  }

  /**
   * Records that a potion effect expired or was removed from a player.
   *
   * @param playerId   UUID string
   * @param effectKey  lowercase Bukkit effect key
   */
  public void onEffectRemove(String playerId, String effectKey) {
    Map<String, Integer> effects = playerEffects.get(playerId);
    if (effects != null) {
      effects.remove(effectKey);
    }
  }

  /**
   * Returns the amplifier of the given effect for a player, or -1 if not active.
   *
   * @param playerId   UUID string
   * @param effectKey  lowercase Bukkit effect key
   * @return  0-indexed amplifier (0 = I, 1 = II, ...) or -1 if not active
   */
  public int getAmplifier(String playerId, String effectKey) {
    Map<String, Integer> effects = playerEffects.get(playerId);
    if (effects == null) return -1;
    return effects.getOrDefault(effectKey, -1);
  }

  /**
   * Returns {@code true} if the player has the given effect active at any level.
   *
   * @param playerId   UUID string
   * @param effectKey  lowercase Bukkit effect key
   */
  public boolean hasEffect(String playerId, String effectKey) {
    return getAmplifier(playerId, effectKey) >= 0;
  }

  /**
   * Returns the speed multiplier from the Speed effect, or {@code 1.0} if none.
   *
   * <p>Vanilla formula: each Speed level adds 20% to base speed.
   * Speed I → ×1.20, Speed II → ×1.40.
   */
  public double speedMultiplier(String playerId) {
    int amp = getAmplifier(playerId, SPEED);
    if (amp < 0) return 1.0;
    return 1.0 + (amp + 1) * 0.20;
  }

  /**
   * Returns the jump-boost horizontal launch multiplier, or {@code 1.0} if none.
   *
   * <p>Jump Boost increases the jump velocity, which also slightly increases
   * maximum horizontal distance per jump.  Approximate: +15% per level.
   */
  public double jumpBoostMultiplier(String playerId) {
    int amp = getAmplifier(playerId, JUMP_BOOST);
    if (amp < 0) return 1.0;
    return 1.0 + (amp + 1) * 0.15;
  }

  /**
   * Returns the haste multiplier for block break speed, or {@code 1.0} if none.
   *
   * <p>Vanilla Haste: each level adds 20% break speed.
   * Haste I → ×1.20, Haste II → ×1.40.
   */
  public double hasteMultiplier(String playerId) {
    int amp = getAmplifier(playerId, HASTE);
    if (amp < 0) return 1.0;
    return 1.0 + (amp + 1) * 0.20;
  }

  /** Clean up all state for a disconnected player. */
  public void onQuit(String playerId) {
    playerEffects.remove(playerId);
  }
}
