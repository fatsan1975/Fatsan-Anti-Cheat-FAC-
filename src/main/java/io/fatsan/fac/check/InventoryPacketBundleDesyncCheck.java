package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class InventoryPacketBundleDesyncCheck extends AbstractBufferedCheck {
  public InventoryPacketBundleDesyncCheck(int limit) { super(limit); }
  @Override public String name() { return "InventoryPacketBundleDesync"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof InventoryClickEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.intervalNanos()<9_000_000L && !e.movingFast();
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"inventory packet bundle desync",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
