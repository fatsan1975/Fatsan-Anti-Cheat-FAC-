package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakCustomItemContextLockCheck extends AbstractBufferedCheck {
  public BlockBreakCustomItemContextLockCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockBreakCustomItemContextLock"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockBreakEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.customItemContext() && e.enchantWeight()>10 && e.intervalNanos()/1_000_000L<80L;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "custom item context cadence lock", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
