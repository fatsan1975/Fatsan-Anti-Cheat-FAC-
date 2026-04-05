package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RotationYawPitchRatioLockCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> streak = new ConcurrentHashMap<>();
  public RotationYawPitchRatioLockCheck(int limit){super(limit);} 
  @Override public String name(){return "RotationYawPitchRatioLock";}
  @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof RotationEvent r)) return CheckResult.clean(name(),category());
    float yaw=Math.abs(r.deltaYaw()), pitch=Math.abs(r.deltaPitch());
    boolean lock=yaw>8F && pitch>0.2F && Math.abs((yaw/(pitch+0.0001F))-3.0F)<0.08F;
    int s=lock?streak.getOrDefault(r.playerId(),0)+1:0; streak.put(r.playerId(),s);
    if(s>=6){int b=incrementBuffer(r.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"yaw/pitch ratio lock",Math.min(1D,b/7D),false);} else coolDown(r.playerId());
    return CheckResult.clean(name(),category());
  }
}
