package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class InventoryIdleIntervalLockCheck extends AbstractBufferedCheck {
  private final Map<String,Long> last=new ConcurrentHashMap<>(); private final Map<String,Integer> streak=new ConcurrentHashMap<>();
  public InventoryIdleIntervalLockCheck(int limit){super(limit);} @Override public String name(){return "InventoryIdleIntervalLock";} @Override public CheckCategory category(){return CheckCategory.INVENTORY;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof InventoryClickEventSignal i)||i.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category()); long ms=i.intervalNanos()/1_000_000L; Long p=last.put(i.playerId(),ms); if(p==null) return CheckResult.clean(name(),category());
    int s=(!i.movingFast()&&Math.abs(ms-p)<=1L&&ms>=45L&&ms<=130L)?streak.getOrDefault(i.playerId(),0)+1:0; streak.put(i.playerId(),s); if(s>=8){int b=incrementBuffer(i.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"idle inventory interval lock",Math.min(1D,b/7D),false);} else coolDown(i.playerId()); return CheckResult.clean(name(),category());}
}
