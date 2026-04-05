package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HitReachSwitchPatternCheck extends AbstractBufferedCheck {
  private final Map<String, Double> lastReach = new ConcurrentHashMap<>();
  public HitReachSwitchPatternCheck(int limit) { super(limit); }
  @Override public String name() { return "HitReachSwitchPattern"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) return CheckResult.clean(name(), category());
    Double prev = lastReach.put(hit.playerId(), hit.reachDistance());
    if (prev != null) {
      boolean suspiciousSwitch = prev < 2.6D && hit.reachDistance() > 3.35D;
      if (suspiciousSwitch) {
        int buf = incrementBuffer(hit.playerId());
        if (overLimit(buf)) return new CheckResult(true, name(), category(), "Abrupt short-to-long reach switching pattern", Math.min(1.0D, buf / 8.0D), false);
      } else coolDown(hit.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
