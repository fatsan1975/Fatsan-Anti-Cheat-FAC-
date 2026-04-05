package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class RotationYawStepGridCheck extends AbstractBufferedCheck {
  public RotationYawStepGridCheck(int limit){super(limit);} 
  @Override public String name(){return "RotationYawStepGrid";}
  @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof RotationEvent r)) return CheckResult.clean(name(),category());
    float y=Math.abs(r.deltaYaw());
    boolean trigger=y>7.5F && Math.abs((y*10F)-Math.round(y*10F))<0.03F;
    if(trigger){int b=incrementBuffer(r.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"yaw grid quantization",Math.min(1D,b/7D),false);} else coolDown(r.playerId());
    return CheckResult.clean(name(),category());
  }
}
