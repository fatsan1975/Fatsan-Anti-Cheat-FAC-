package io.fatsan.fac.config;

import java.util.Set;

/**
 * Per-check configuration. Every check reads its runtime parameters from this
 * record instead of hardcoded constants.
 *
 * @param enabled        whether the check is active
 * @param bufferLimit    how many consecutive suspicious samples before flagging
 * @param threshold      primary detection threshold (meaning varies per check)
 * @param minVl          minimum violation level before an actionable result is emitted
 * @param severityCap    maximum severity this check can report [0.0–1.0]
 * @param worlds         if non-empty, check only runs in these worlds
 * @param exemptPermissions additional permission nodes that exempt a player
 */
public record CheckSettings(
    boolean enabled,
    int bufferLimit,
    double threshold,
    int minVl,
    double severityCap,
    Set<String> worlds,
    Set<String> exemptPermissions) {

  /** Sensible defaults — enabled, buffer=6, no world filter, no extra permissions. */
  public static CheckSettings defaults() {
    return new CheckSettings(true, 6, 0.0, 6, 1.0, Set.of(), Set.of());
  }

  /** Returns true if worlds is empty (all worlds) or contains the given world name. */
  public boolean activeInWorld(String worldName) {
    return worlds.isEmpty() || worlds.contains(worldName);
  }
}
