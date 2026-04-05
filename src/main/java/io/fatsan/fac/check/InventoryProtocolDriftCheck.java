package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class InventoryProtocolDriftCheck extends AbstractBufferedCheck {
  public InventoryProtocolDriftCheck(int limit){super(limit);} @Override public String name(){return "InventoryProtocolDrift";} @Override public CheckCategory category(){return CheckCategory.INVENTORY;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof InventoryClickEventSignal i)||i.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=i.intervalNanos()<20_000_000L && !i.movingFast(); if(t){int b=incrementBuffer(i.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"inventory protocol drift",Math.min(1D,b/7D),false);} else coolDown(i.playerId()); return CheckResult.clean(name(),category()); }
}
