package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class InventoryMovingFixedStepCheck extends AbstractBufferedCheck {
  private final Map<String,Long> last=new ConcurrentHashMap<>();
  public InventoryMovingFixedStepCheck(int limit){super(limit);} @Override public String name(){return "InventoryMovingFixedStep";} @Override public CheckCategory category(){return CheckCategory.INVENTORY;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof InventoryClickEventSignal i)||i.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category()); long ms=i.intervalNanos()/1_000_000L; Long pv=last.put(i.playerId(),ms);
    boolean t=i.movingFast() && pv!=null && Math.abs(ms-pv)<=1L && ms<95L; if(t){int b=incrementBuffer(i.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"moving inventory fixed-step",Math.min(1D,b/7D),false);} else coolDown(i.playerId()); return CheckResult.clean(name(),category());}
}
