package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakCommandLoreAnomalyCheck extends AbstractBufferedCheck {
  public BlockBreakCommandLoreAnomalyCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockBreakCommandLoreAnomaly"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockBreakEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.customItemContext() && e.enchantWeight()>=6 && e.intervalNanos()<72_000_000L;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"command lore anomaly break",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
