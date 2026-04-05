package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class InventoryPacketOrderBurstCheck extends AbstractBufferedCheck {
  public InventoryPacketOrderBurstCheck(int limit) { super(limit); }
  @Override public String name() { return "InventoryPacketOrderBurst"; }
  @Override public CheckCategory category() { return CheckCategory.INVENTORY; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof InventoryClickEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.intervalNanos()<7_000_000L;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "inventory packet order burst", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
