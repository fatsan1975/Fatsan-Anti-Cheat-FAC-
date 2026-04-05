package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HitIntervalBurstCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> burstStreak = new ConcurrentHashMap<>();

  public HitIntervalBurstCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "HitIntervalBurst";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit) || hit.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    long ms = hit.intervalNanos() / 1_000_000L;
    if (ms > 0 && ms < 35L) {
      int streak = burstStreak.getOrDefault(hit.playerId(), 0) + 1;
      burstStreak.put(hit.playerId(), streak);
      if (streak >= 4) {
        int buf = incrementBuffer(hit.playerId());
        if (overLimit(buf)) {
          return new CheckResult(true, name(), category(), "Sustained ultra-low hit interval burst", Math.min(1.0D, buf / 8.0D), true);
        }
      }
    } else {
      burstStreak.put(hit.playerId(), 0);
      coolDown(hit.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
