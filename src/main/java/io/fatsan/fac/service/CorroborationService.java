package io.fatsan.fac.service;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CorroborationService {
  private final long windowMillis;
  private final int minDistinctCategories;
  private final int minEvents;
  private final Map<String, Deque<Entry>> entriesByPlayer = new ConcurrentHashMap<>();

  public CorroborationService(long windowMillis, int minDistinctCategories, int minEvents) {
    this.windowMillis = windowMillis;
    this.minDistinctCategories = minDistinctCategories;
    this.minEvents = minEvents;
  }

  public void record(String playerId, CheckResult result) {
    Deque<Entry> deque = entriesByPlayer.computeIfAbsent(playerId, key -> new ArrayDeque<>());
    long now = System.currentTimeMillis();
    synchronized (deque) {
      deque.addLast(new Entry(now, result.category()));
      evictOld(deque, now);
    }
  }

  public boolean isCorroborated(String playerId) {
    Deque<Entry> deque = entriesByPlayer.get(playerId);
    if (deque == null) {
      return false;
    }
    long now = System.currentTimeMillis();
    synchronized (deque) {
      evictOld(deque, now);
      if (deque.size() < minEvents) {
        return false;
      }
      Set<CheckCategory> categories = new HashSet<>();
      for (Entry entry : deque) {
        categories.add(entry.category());
      }
      return categories.size() >= minDistinctCategories;
    }
  }

  private void evictOld(Deque<Entry> deque, long now) {
    while (!deque.isEmpty()) {
      Entry head = deque.peekFirst();
      if (head == null || now - head.timeMillis() <= windowMillis) {
        break;
      }
      deque.pollFirst();
    }
  }

  /** Removes all corroboration evidence for the given player. Called on disconnect. */
  public void clearPlayer(String playerId) {
    entriesByPlayer.remove(playerId);
  }

  private record Entry(long timeMillis, CheckCategory category) {}
}
