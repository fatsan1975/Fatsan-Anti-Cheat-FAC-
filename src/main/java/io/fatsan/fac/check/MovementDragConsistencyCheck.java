package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MovementDragConsistencyCheck extends AbstractBufferedCheck {
  private final Map<String, Double> last = new ConcurrentHashMap<>();
  public MovementDragConsistencyCheck(int limit){super(limit);} 
  @Override public String name(){return "MovementDragConsistency";}
  @Override public CheckCategory category(){return CheckCategory.MOVEMENT;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof MovementEvent m) || m.onGround() || m.gliding() || m.inVehicle()) return CheckResult.clean(name(),category());
    Double prev=last.put(m.playerId(),m.deltaXZ());
    boolean trigger= prev!=null && prev>0.30D && m.deltaXZ()>prev+0.06D;
    if(trigger){int b=incrementBuffer(m.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"air drag consistency break",Math.min(1D,b/7D),false);} else coolDown(m.playerId());
    return CheckResult.clean(name(),category());
  }
}
