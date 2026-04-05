package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class CombatCriticalSpamWindowCheck extends AbstractBufferedCheck {
  private final Map<String,Integer> streak=new ConcurrentHashMap<>();
  public CombatCriticalSpamWindowCheck(int limit){super(limit);} @Override public String name(){return "CombatCriticalSpamWindow";} @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof CombatHitEvent h)||h.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    int s=(h.criticalLike()&&h.intervalNanos()/1_000_000L<110L)?streak.getOrDefault(h.playerId(),0)+1:0; streak.put(h.playerId(),s); if(s>=7){int b=incrementBuffer(h.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"critical spam window",Math.min(1D,b/7D),false);} else coolDown(h.playerId()); return CheckResult.clean(name(),category());}
}
