package io.fatsan.fac.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSignalTracker {
  private final Map<UUID, Long> lastMove = new ConcurrentHashMap<>();
  private final Map<UUID, Long> lastHit = new ConcurrentHashMap<>();
  private final Map<UUID, Long> lastPlace = new ConcurrentHashMap<>();
  private final Map<UUID, Long> lastBreak = new ConcurrentHashMap<>();
  private final Map<UUID, Long> lastInventoryClick = new ConcurrentHashMap<>();
  private final Map<UUID, Long> lastKeepAliveSample = new ConcurrentHashMap<>();

  public long intervalMove(UUID id, long now) { return interval(lastMove, id, now); }
  public long intervalHit(UUID id, long now) { return interval(lastHit, id, now); }
  public long intervalPlace(UUID id, long now) { return interval(lastPlace, id, now); }
  public long intervalBreak(UUID id, long now) { return interval(lastBreak, id, now); }
  public long intervalInventoryClick(UUID id, long now) { return interval(lastInventoryClick, id, now); }


  public boolean shouldSampleKeepAlive(UUID id, long now, int minIntervalMillis) {
    long minIntervalNanos = Math.max(1L, minIntervalMillis) * 1_000_000L;
    Long previous = lastKeepAliveSample.get(id);
    if (previous == null || now - previous >= minIntervalNanos) {
      lastKeepAliveSample.put(id, now);
      return true;
    }
    return false;
  }

  private static long interval(Map<UUID, Long> map, UUID id, long now) {
    Long old = map.put(id, now);
    if (old == null) return Long.MAX_VALUE;
    return Math.max(0L, now - old);
  }

  public void clear(UUID id) {
    lastMove.remove(id);
    lastHit.remove(id);
    lastPlace.remove(id);
    lastBreak.remove(id);
    lastInventoryClick.remove(id);
    lastKeepAliveSample.remove(id);
  }
}
