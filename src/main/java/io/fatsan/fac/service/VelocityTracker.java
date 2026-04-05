package io.fatsan.fac.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player velocity vectors and expected post-knockback state.
 *
 * <p>Used by AntiKBCheck to compare expected knockback velocity with the
 * velocity observed after an entity damage event.  Also provides the
 * velocity snapshot needed for physics-based movement validation.
 *
 * <p>Thread-safe: all state stored in ConcurrentHashMap with volatile records.
 */
public final class VelocityTracker {

  private final Map<String, VelocitySnapshot> velocities = new ConcurrentHashMap<>();
  private final Map<String, PendingKnockback> pendingKnockback = new ConcurrentHashMap<>();

  /** Stores the most recent observed velocity for a player. */
  public void recordVelocity(String playerId, double vx, double vy, double vz) {
    velocities.put(playerId, new VelocitySnapshot(vx, vy, vz, System.nanoTime()));
  }

  /** Returns the last recorded velocity snapshot, or ZERO if none. */
  public VelocitySnapshot lastVelocity(String playerId) {
    return velocities.getOrDefault(playerId, VelocitySnapshot.ZERO);
  }

  /**
   * Records expected knockback to be applied to the given player.
   * Called when a player is hit; the expected Y velocity after knockback is
   * approximately 0.4 (vanilla default when not on ground).
   */
  public void expectKnockback(String playerId, double expectedVy, double expectedHorizontal) {
    pendingKnockback.put(playerId, new PendingKnockback(expectedVy, expectedHorizontal, System.nanoTime()));
  }

  /**
   * Consumes and returns the pending knockback expectation for the player.
   * Returns null if no knockback is pending or it has expired (> 500ms).
   */
  public PendingKnockback consumeKnockback(String playerId) {
    PendingKnockback kb = pendingKnockback.remove(playerId);
    if (kb == null) return null;
    long age = System.nanoTime() - kb.timestamp();
    if (age > 500_000_000L) return null; // expired after 500ms
    return kb;
  }

  /** Clears all state for the given player. Called on disconnect. */
  public void clearPlayer(String playerId) {
    velocities.remove(playerId);
    pendingKnockback.remove(playerId);
  }

  public record VelocitySnapshot(double vx, double vy, double vz, long timestamp) {
    public static final VelocitySnapshot ZERO = new VelocitySnapshot(0, 0, 0, 0);

    public double horizontalMagnitude() {
      return Math.sqrt(vx * vx + vz * vz);
    }
  }

  public record PendingKnockback(double expectedVy, double expectedHorizontal, long timestamp) {}
}
