package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryMoveStopClickDesyncCheck extends AbstractBufferedCheck {
  private final Map<String,Boolean> lastMoving=new ConcurrentHashMap<>();
  public InventoryMoveStopClickDesyncCheck(int limit){super(limit);} 
  @Override public String name(){return "InventoryMoveStopClickDesync";}
  @Override public CheckCategory category(){return CheckCategory.INVENTORY;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof InventoryClickEventSignal i)) return CheckResult.clean(name(),category());
    Boolean prev=lastMoving.put(i.playerId(),i.movingFast());
    boolean trigger=prev!=null && prev && !i.movingFast() && i.intervalNanos()!=Long.MAX_VALUE && (i.intervalNanos()/1_000_000L)<35L;
    if(trigger){int b=incrementBuffer(i.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"move-stop click desync",Math.min(1D,b/7D),false);} else coolDown(i.playerId());
    return CheckResult.clean(name(),category());
  }
}
