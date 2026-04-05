package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PingOscillationSpoofCheck extends AbstractBufferedCheck {
  private final Map<String, Long> lastPing = new ConcurrentHashMap<>();
  private final Map<String, Integer> lastDiffSign = new ConcurrentHashMap<>();
  private final Map<String, Integer> oscillationStreak = new ConcurrentHashMap<>();

  public PingOscillationSpoofCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "PingOscillationSpoof";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof KeepAliveSignal keepAlive)) {
      return CheckResult.clean(name(), category());
    }

    String playerId = keepAlive.playerId();
    long currentPing = keepAlive.pingMillis();
    Long previousPing = lastPing.put(playerId, currentPing);
    if (previousPing == null) {
      return CheckResult.clean(name(), category());
    }

    long diff = currentPing - previousPing;
    int sign = Long.compare(diff, 0L);
    int previousSign = lastDiffSign.getOrDefault(playerId, 0);
    lastDiffSign.put(playerId, sign);

    int streak = oscillationStreak.getOrDefault(playerId, 0);
    if (Math.abs(diff) >= 180L && sign != 0 && previousSign != 0 && sign != previousSign) {
      streak++;
    } else {
      streak = Math.max(0, streak - 1);
      coolDown(playerId);
    }
    oscillationStreak.put(playerId, streak);

    if (streak >= 3) {
      int buf = incrementBuffer(playerId);
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "High-amplitude alternating ping oscillation pattern",
            Math.min(1.0D, buf / 8.0D),
            false);
      }
    }

    return CheckResult.clean(name(), category());
  }
}
