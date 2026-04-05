package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakCommandAttributeBoostCheck extends AbstractBufferedCheck {
  public BlockBreakCommandAttributeBoostCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockBreakCommandAttributeBoost"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockBreakEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.customItemContext() && e.itemMovementSpeedBonus()>0.12D && e.intervalNanos()/1_000_000L<70L;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "command-like attribute boosted breaking", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
