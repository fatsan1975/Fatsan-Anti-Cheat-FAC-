package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoClickerCadenceCheck extends AbstractBufferedCheck {
  private final Map<String, Long> lastInterval = new ConcurrentHashMap<>();

  public AutoClickerCadenceCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "AutoClickerCadence";
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

    long current = hit.intervalNanos();
    Long previous = lastInterval.put(hit.playerId(), current);
    if (previous != null) {
      long delta = Math.abs(previous - current);
      long intervalMs = current / 1_000_000L;
      if (intervalMs <= 55 && delta < 3_000_000L) {
        int buf = incrementBuffer(hit.playerId());
        if (overLimit(buf)) {
          return new CheckResult(
              true,
              name(),
              category(),
              "Unnaturally stable click cadence",
              Math.min(1.0D, buf / 8.0D),
              true);
        }
      } else {
        coolDown(hit.playerId());
      }
    }

    return CheckResult.clean(name(), category());
  }
}
