package io.fatsan.fac.service;

import io.fatsan.fac.model.CheckResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived per-player result cache for expensive checks.
 *
 * <h2>Why caching is needed on large Folia servers</h2>
 *
 * <p>On busy Folia servers with 200+ players, some checks are computationally
 * expensive and run on every single movement event (20 times per second per
 * player):
 *
 * <ul>
 *   <li>{@code ForcefieldCheck} computes circular spread across a bearing array
 *       on every hit event in a busy PvP scenario.</li>
 *   <li>{@code XRayHeuristicCheck} iterates a deque of 20 materials on every
 *       block break.</li>
 *   <li>{@code FreecamDesyncCheck} computes 3D Euclidean distance on every
 *       movement packet.</li>
 * </ul>
 *
 * <p>The cache stores the most recent {@link CheckResult} for a
 * (playerId, checkName) pair with a TTL of {@value #DEFAULT_TTL_MS}ms.
 * If the same check fires for the same player within the TTL, the cached result
 * is returned instead of re-evaluating.
 *
 * <h2>When to use the cache</h2>
 *
 * <p>Only use this cache for checks where:
 * <ul>
 *   <li>The check's internal state changes slowly (once every 50ms or slower).</li>
 *   <li>The check is called on high-frequency events (movement, every tick).</li>
 *   <li>The check involves map lookups or statistical computation.</li>
 * </ul>
 *
 * <p>Do NOT cache checks that depend on every event independently (e.g.,
 * {@code NoSwingCheck} which inspects per-hit swing state).
 *
 * <h2>Thread safety</h2>
 *
 * <p>Entries are stored in {@code ConcurrentHashMap} keyed by
 * {@code playerId + ":" + checkName}.  No synchronisation is needed — stale
 * reads are acceptable (the cache is an optimisation, not a correctness
 * guarantee).
 *
 * <h2>Example usage in a check</h2>
 *
 * <pre>{@code
 * CheckResult cached = resultCache.get(playerId, name());
 * if (cached != null) return cached;
 * // ... expensive computation ...
 * resultCache.put(playerId, name(), result);
 * return result;
 * }</pre>
 */
public final class CheckResultCache {

  /** Default TTL in milliseconds. */
  public static final long DEFAULT_TTL_MS = 50L;
  private static final long DEFAULT_TTL_NS = DEFAULT_TTL_MS * 1_000_000L;

  private record Entry(CheckResult result, long expiresNanos) {}

  private final Map<String, Entry> cache = new ConcurrentHashMap<>();

  /**
   * Returns the cached result for (playerId, checkName) if it has not expired,
   * or {@code null} if no valid cache entry exists.
   */
  public CheckResult get(String playerId, String checkName) {
    Entry entry = cache.get(key(playerId, checkName));
    if (entry == null) return null;
    if (System.nanoTime() > entry.expiresNanos()) {
      cache.remove(key(playerId, checkName), entry);
      return null;
    }
    return entry.result();
  }

  /**
   * Stores a result for (playerId, checkName) with the default TTL of
   * {@value #DEFAULT_TTL_MS}ms.
   */
  public void put(String playerId, String checkName, CheckResult result) {
    put(playerId, checkName, result, DEFAULT_TTL_NS);
  }

  /**
   * Stores a result for (playerId, checkName) with a custom TTL in nanoseconds.
   */
  public void put(String playerId, String checkName, CheckResult result, long ttlNanos) {
    long expires = System.nanoTime() + ttlNanos;
    cache.put(key(playerId, checkName), new Entry(result, expires));
  }

  /** Evicts all cached entries for a disconnected player. */
  public void evictPlayer(String playerId) {
    // ConcurrentHashMap supports concurrent removal during iteration
    String prefix = playerId + ":";
    cache.keySet().removeIf(k -> k.startsWith(prefix));
  }

  /** Evicts all entries (e.g., on reload). */
  public void evictAll() {
    cache.clear();
  }

  private static String key(String playerId, String checkName) {
    return playerId + ":" + checkName;
  }
}
