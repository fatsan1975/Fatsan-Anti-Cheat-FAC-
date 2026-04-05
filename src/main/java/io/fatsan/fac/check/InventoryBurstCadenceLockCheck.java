package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryBurstCadenceLockCheck extends AbstractBufferedCheck {
  private final Map<String,Long> lastMs=new ConcurrentHashMap<>();
  private final Map<String,Integer> streak=new ConcurrentHashMap<>();
  public InventoryBurstCadenceLockCheck(int limit){super(limit);} 
  @Override public String name(){return "InventoryBurstCadenceLock";}
  @Override public CheckCategory category(){return CheckCategory.INVENTORY;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof InventoryClickEventSignal i) || i.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    long ms=i.intervalNanos()/1_000_000L; Long prev=lastMs.put(i.playerId(),ms); if(prev==null) return CheckResult.clean(name(),category());
    boolean lock=Math.abs(ms-prev)<=1L && ms>=25L && ms<=70L;
    int s=lock?streak.getOrDefault(i.playerId(),0)+1:0; streak.put(i.playerId(),s);
    if(s>=8){int b=incrementBuffer(i.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"inventory burst cadence lock",Math.min(1D,b/7D),false);} else coolDown(i.playerId());
    return CheckResult.clean(name(),category());
  }
}
