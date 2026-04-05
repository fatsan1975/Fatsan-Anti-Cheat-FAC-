package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class MovementAttributeCouplingCheck extends AbstractBufferedCheck {
  public MovementAttributeCouplingCheck(int limit){super(limit);} @Override public String name(){return "MovementAttributeCoupling";} @Override public CheckCategory category(){return CheckCategory.MOVEMENT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof MovementEvent m)||m.gliding()||m.inVehicle()) return CheckResult.clean(name(),category());
    boolean t=m.onGround() && m.deltaXZ()>1.05D; if(t){int b=incrementBuffer(m.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"movement-attribute coupling anomaly",Math.min(1D,b/7D),true);} else coolDown(m.playerId()); return CheckResult.clean(name(),category()); }
}
