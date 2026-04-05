package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class CombatCriticalGroundMismatchCheck extends AbstractBufferedCheck {
  public CombatCriticalGroundMismatchCheck(int limit){super(limit);} 
  @Override public String name(){return "CombatCriticalGroundMismatch";}
  @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof CombatHitEvent h)) return CheckResult.clean(name(),category());
    boolean trigger=h.criticalLike() && h.onGround() && h.fallDistance()==0.0F;
    if(trigger){int b=incrementBuffer(h.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"critical-ground mismatch",Math.min(1D,b/6D),true);} else coolDown(h.playerId());
    return CheckResult.clean(name(),category());
  }
}
