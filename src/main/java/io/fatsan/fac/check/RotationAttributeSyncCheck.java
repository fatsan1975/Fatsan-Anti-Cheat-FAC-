package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class RotationAttributeSyncCheck extends AbstractBufferedCheck {
  public RotationAttributeSyncCheck(int limit){super(limit);} @Override public String name(){return "RotationAttributeSync";} @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof RotationEvent r)) return CheckResult.clean(name(),category());
    boolean t=Math.abs(r.deltaYaw())>40F && Math.abs(r.deltaPitch())<0.02F; if(t){int b=incrementBuffer(r.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"rotation sync lock",Math.min(1D,b/7D),false);} else coolDown(r.playerId()); return CheckResult.clean(name(),category()); }
}
