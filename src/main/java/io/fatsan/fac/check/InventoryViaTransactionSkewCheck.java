package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class InventoryViaTransactionSkewCheck extends AbstractBufferedCheck {
  public InventoryViaTransactionSkewCheck(int limit) { super(limit); }
  @Override public String name() { return "InventoryViaTransactionSkew"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof InventoryClickEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.intervalNanos()<12_000_000L && e.movingFast();
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "inventory transaction skew while moving", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
