package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MovementGroundTransitionBurstCheck extends AbstractBufferedCheck {
  private final Map<String, Boolean> lastGround = new ConcurrentHashMap<>();
  public MovementGroundTransitionBurstCheck(int limit){super(limit);} 
  @Override public String name(){return "MovementGroundTransitionBurst";}
  @Override public CheckCategory category(){return CheckCategory.MOVEMENT;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof MovementEvent m) || m.gliding() || m.inVehicle()) return CheckResult.clean(name(),category());
    Boolean prev=lastGround.put(m.playerId(),m.onGround());
    boolean trigger=prev!=null && prev && !m.onGround() && m.deltaXZ()>0.95D;
    if(trigger){int b=incrementBuffer(m.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"ground transition horizontal burst",Math.min(1D,b/7D),true);} else coolDown(m.playerId());
    return CheckResult.clean(name(),category());
  }
}
