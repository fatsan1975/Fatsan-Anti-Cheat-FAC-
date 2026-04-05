package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class RotationPitchStepGridCheck extends AbstractBufferedCheck {
  public RotationPitchStepGridCheck(int limit){super(limit);} 
  @Override public String name(){return "RotationPitchStepGrid";}
  @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof RotationEvent r)) return CheckResult.clean(name(),category());
    float p=Math.abs(r.deltaPitch());
    boolean trigger=p>2.0F && Math.abs((p*20F)-Math.round(p*20F))<0.04F;
    if(trigger){int b=incrementBuffer(r.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"pitch grid quantization",Math.min(1D,b/7D),false);} else coolDown(r.playerId());
    return CheckResult.clean(name(),category());
  }
}
