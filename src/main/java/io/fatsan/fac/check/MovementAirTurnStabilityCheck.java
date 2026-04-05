package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class MovementAirTurnStabilityCheck extends AbstractBufferedCheck {
  public MovementAirTurnStabilityCheck(int limit){super(limit);} @Override public String name(){return "MovementAirTurnStability";} @Override public CheckCategory category(){return CheckCategory.MOVEMENT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof MovementEvent m) || m.onGround()||m.gliding()||m.inVehicle()) return CheckResult.clean(name(),category());
    boolean trigger=m.deltaXZ()>0.42D && Math.abs(m.deltaY())<0.01D; if(trigger){int b=incrementBuffer(m.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"air turn stability lock",Math.min(1D,b/7D),false);} else coolDown(m.playerId()); return CheckResult.clean(name(),category());}
}
