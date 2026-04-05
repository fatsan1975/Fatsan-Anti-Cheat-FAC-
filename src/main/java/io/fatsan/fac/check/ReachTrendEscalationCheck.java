package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReachTrendEscalationCheck extends AbstractBufferedCheck {
  private final Map<String, Double> lastReach = new ConcurrentHashMap<>();
  private final Map<String, Integer> upStreak = new ConcurrentHashMap<>();
  public ReachTrendEscalationCheck(int limit) { super(limit); }
  @Override public String name() { return "ReachTrendEscalation"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) return CheckResult.clean(name(), category());
    Double prev = lastReach.put(hit.playerId(), hit.reachDistance());
    if (prev != null && hit.reachDistance() > prev + 0.18D && hit.reachDistance() > 3.1D) {
      int st = upStreak.getOrDefault(hit.playerId(), 0) + 1; upStreak.put(hit.playerId(), st);
      if (st >= 3) {
        int buf = incrementBuffer(hit.playerId());
        if (overLimit(buf)) return new CheckResult(true, name(), category(), "Escalating reach trend across consecutive hits", Math.min(1.0D, buf / 8.0D), true);
      }
    } else { upStreak.put(hit.playerId(), 0); coolDown(hit.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
