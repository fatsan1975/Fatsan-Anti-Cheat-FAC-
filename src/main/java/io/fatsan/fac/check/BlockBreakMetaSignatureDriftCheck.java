package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakMetaSignatureDriftCheck extends AbstractBufferedCheck {
  public BlockBreakMetaSignatureDriftCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockBreakMetaSignatureDrift"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockBreakEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.customItemContext() && e.itemMovementSpeedBonus()>0.18D && e.efficiencyLevel()<=1;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"meta signature drift break",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
