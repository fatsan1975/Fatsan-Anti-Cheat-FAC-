package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakDeepContextAmplifierCheck extends AbstractBufferedCheck {
  public BlockBreakDeepContextAmplifierCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockBreakDeepContextAmplifier"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockBreakEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.customItemContext() && e.hasteAmplifier()>=0 && e.enchantWeight()>9;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"deep context amplifier",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
