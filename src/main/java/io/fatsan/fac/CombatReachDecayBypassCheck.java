package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatReachDecayBypassCheck extends AbstractBufferedCheck {
  private final Map<String, Double> last = new ConcurrentHashMap<>();
  public CombatReachDecayBypassCheck(int limit){super(limit);} 
  @Override public String name(){return "CombatReachDecayBypass";}
  @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof CombatHitEvent h)) return CheckResult.clean(name(),category());
    Double prev=last.put(h.playerId(),h.reachDistance());
    boolean trigger=prev!=null && prev>3.0D && h.reachDistance()>3.0D && Math.abs(h.reachDistance()-prev)<0.015D;
    if(trigger){int b=incrementBuffer(h.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"reach decay bypass lock",Math.min(1D,b/7D),true);} else coolDown(h.playerId());
    return CheckResult.clean(name(),category());
  }
}
