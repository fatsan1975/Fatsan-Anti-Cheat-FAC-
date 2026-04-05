package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class CombatReachContextLockCheck extends AbstractBufferedCheck {
  private final Map<String,Double> last=new ConcurrentHashMap<>();
  public CombatReachContextLockCheck(int limit){super(limit);} @Override public String name(){return "CombatReachContextLock";} @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof CombatHitEvent h)) return CheckResult.clean(name(),category()); Double p=last.put(h.playerId(),h.reachDistance());
    boolean t=p!=null && Math.abs(h.reachDistance()-p)<0.01D && h.reachDistance()>3.2D; if(t){int b=incrementBuffer(h.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"reach context lock",Math.min(1D,b/7D),false);} else coolDown(h.playerId()); return CheckResult.clean(name(),category()); }
}
