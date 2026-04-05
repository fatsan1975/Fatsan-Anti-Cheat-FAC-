package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class CombatViaWindowSmearCheck extends AbstractBufferedCheck {
  public CombatViaWindowSmearCheck(int limit) { super(limit); }
  @Override public String name() { return "CombatViaWindowSmear"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof CombatHitEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = e.reachDistance()>3.25D && e.intervalNanos()<95_000_000L && e.criticalLike();
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"via window smear",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
