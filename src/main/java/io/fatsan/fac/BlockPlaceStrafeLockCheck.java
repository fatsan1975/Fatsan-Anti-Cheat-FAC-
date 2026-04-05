package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockPlaceStrafeLockCheck extends AbstractBufferedCheck {
  private final Map<String,Integer> streak=new ConcurrentHashMap<>();
  public BlockPlaceStrafeLockCheck(int limit){super(limit);} 
  @Override public String name(){return "BlockPlaceStrafeLock";}
  @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof BlockPlaceEventSignal p) || p.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    long ms=p.intervalNanos()/1_000_000L;
    boolean pat=p.horizontalSpeed()>0.33D && p.sprinting() && ms>=35L && ms<=65L;
    int s=pat?streak.getOrDefault(p.playerId(),0)+1:0; streak.put(p.playerId(),s);
    if(s>=7){int b=incrementBuffer(p.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"strafe-place lock pattern",Math.min(1D,b/7D),true);} else coolDown(p.playerId());
    return CheckResult.clean(name(),category());
  }
}
