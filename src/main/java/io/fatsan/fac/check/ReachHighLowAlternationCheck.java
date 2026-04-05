package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReachHighLowAlternationCheck extends AbstractBufferedCheck {
  private final Map<String, Double> last = new ConcurrentHashMap<>();
  public ReachHighLowAlternationCheck(int limit) { super(limit); }
  @Override public String name() { return "ReachHighLowAlternation"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) return CheckResult.clean(name(), category());
    boolean trigger = false;
    Double prev=last.put(hit.playerId(),hit.reachDistance()); if(prev!=null){ boolean alt=(prev<2.7D && hit.reachDistance()>3.35D)||(prev>3.35D && hit.reachDistance()<2.7D); if(alt) trigger=true; }
    if (trigger) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "ReachHighLowAlternation anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(hit.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
