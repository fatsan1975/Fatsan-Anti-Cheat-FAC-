package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakHasteNoEnchantCheck extends AbstractBufferedCheck {
  public BlockBreakHasteNoEnchantCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockBreakHasteNoEnchant"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockBreakEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.hasteAmplifier()>=1 && e.efficiencyLevel()==0 && e.intervalNanos()/1_000_000L<65L;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "haste break speed without efficiency", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
