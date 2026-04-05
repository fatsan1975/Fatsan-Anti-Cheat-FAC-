package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class CombatProtocolTimingMismatchCheck extends AbstractBufferedCheck {
  public CombatProtocolTimingMismatchCheck(int limit){super(limit);} @Override public String name(){return "CombatProtocolTimingMismatch";} @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof CombatHitEvent h)||h.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=h.intervalNanos()<35_000_000L && h.reachDistance()>3.0D; if(t){int b=incrementBuffer(h.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"combat protocol timing mismatch",Math.min(1D,b/7D),true);} else coolDown(h.playerId()); return CheckResult.clean(name(),category()); }
}
