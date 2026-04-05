package io.fatsan.fac.check.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FixedStepWindowTracker {
  private final Map<String, Long> lastValue = new ConcurrentHashMap<>();
  private final Map<String, Integer> streaks = new ConcurrentHashMap<>();

  public int record(String playerId, long currentValue, long tolerance, long minInclusive, long maxInclusive) {
    Long previous = lastValue.put(playerId, currentValue);
    if (previous == null) {
      streaks.remove(playerId);
      return 0;
    }

    boolean matched =
        currentValue >= minInclusive
            && currentValue <= maxInclusive
            && Math.abs(previous - currentValue) <= tolerance;
    if (!matched) {
      streaks.remove(playerId);
      return 0;
    }

    int next = streaks.getOrDefault(playerId, 0) + 1;
    streaks.put(playerId, next);
    return next;
  }

  public void clear(String playerId) {
    lastValue.remove(playerId);
    streaks.remove(playerId);
  }
}
