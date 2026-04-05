package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CriticalCadenceAbuseCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> rapidCriticalStreak = new ConcurrentHashMap<>();

  public CriticalCadenceAbuseCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "CriticalCadenceAbuse";
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

    long intervalMs = hit.intervalNanos() / 1_000_000L;
    boolean suspiciousCritical =
        hit.criticalLike() && intervalMs <= 95L && !hit.onGround() && hit.fallDistance() >= 0.08F;

    if (suspiciousCritical) {
      int streak = rapidCriticalStreak.getOrDefault(hit.playerId(), 0) + 1;
      rapidCriticalStreak.put(hit.playerId(), streak);
      if (streak >= 3) {
        int buf = incrementBuffer(hit.playerId());
        if (overLimit(buf)) {
          return new CheckResult(
              true,
              name(),
              category(),
              "Sustained rapid critical-like hit cadence",
              Math.min(1.0D, buf / 8.0D),
              false);
        }
      }
    } else {
      rapidCriticalStreak.put(hit.playerId(), 0);
      coolDown(hit.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
