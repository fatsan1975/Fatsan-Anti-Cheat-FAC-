package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class InventoryViaRewriteSkewCheck extends AbstractBufferedCheck {
  public InventoryViaRewriteSkewCheck(int limit) { super(limit); }
  @Override public String name() { return "InventoryViaRewriteSkew"; }
  @Override public CheckCategory category() { return CheckCategory.INVENTORY; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof InventoryClickEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.intervalNanos()<14_000_000L && e.movingFast();
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"inventory via rewrite skew",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
