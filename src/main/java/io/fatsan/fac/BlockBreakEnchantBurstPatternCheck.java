package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakEnchantBurstPatternCheck extends AbstractBufferedCheck {
  public BlockBreakEnchantBurstPatternCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockBreakEnchantBurstPattern"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockBreakEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.enchantWeight()>=12 && e.intervalNanos()/1_000_000L<60L;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "high enchant burst break", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
