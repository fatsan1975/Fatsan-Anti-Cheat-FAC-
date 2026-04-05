package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class RotationZeroPitchBurstCheck extends AbstractBufferedCheck {
  public RotationZeroPitchBurstCheck(int limit){super(limit);} @Override public String name(){return "RotationZeroPitchBurst";} @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof RotationEvent r)) return CheckResult.clean(name(),category());
    boolean t=Math.abs(r.deltaYaw())>20F && Math.abs(r.deltaPitch())<0.0001F; if(t){int b=incrementBuffer(r.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"zero-pitch yaw burst",Math.min(1D,b/7D),false);} else coolDown(r.playerId()); return CheckResult.clean(name(),category());}
}
