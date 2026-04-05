package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CriticalReachCouplingCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public CriticalReachCouplingCheck(int limit) { super(limit); }
  @Override public String name() { return "CriticalReachCoupling"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) return CheckResult.clean(name(), category());
    boolean trigger = false;
    if (hit.criticalLike() && hit.reachDistance()>3.3D) trigger=true;
    if (trigger) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "CriticalReachCoupling anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(hit.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
