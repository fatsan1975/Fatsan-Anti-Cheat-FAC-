package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockPlaceSpeedStepCheck extends AbstractBufferedCheck {
  private final Map<String,Long> lastMs=new ConcurrentHashMap<>();
  public BlockPlaceSpeedStepCheck(int limit){super(limit);} 
  @Override public String name(){return "BlockPlaceSpeedStep";}
  @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof BlockPlaceEventSignal p) || p.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    long ms=p.intervalNanos()/1_000_000L; Long prev=lastMs.put(p.playerId(),ms);
    boolean trig=prev!=null && Math.abs(prev-ms)<=1L && p.horizontalSpeed()>0.20D && ms<90L;
    if(trig){int b=incrementBuffer(p.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"place speed step lock",Math.min(1D,b/7D),false);} else coolDown(p.playerId());
    return CheckResult.clean(name(),category());
  }
}
