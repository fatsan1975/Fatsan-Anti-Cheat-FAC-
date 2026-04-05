package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class MovementLowIntervalHighDistanceCheck extends AbstractBufferedCheck {
  public MovementLowIntervalHighDistanceCheck(int limit){super(limit);} 
  @Override public String name(){return "MovementLowIntervalHighDistance";}
  @Override public CheckCategory category(){return CheckCategory.MOVEMENT;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof MovementEvent m) || m.intervalNanos()==Long.MAX_VALUE || m.gliding() || m.inVehicle()) return CheckResult.clean(name(),category());
    boolean trigger=m.intervalNanos()<7_000_000L && m.deltaXZ()>0.28D;
    if(trigger){int b=incrementBuffer(m.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"low-interval high-distance move",Math.min(1D,b/6D),true);} else coolDown(m.playerId());
    return CheckResult.clean(name(),category());
  }
}
