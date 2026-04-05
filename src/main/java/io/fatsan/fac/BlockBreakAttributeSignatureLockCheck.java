package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakAttributeSignatureLockCheck extends AbstractBufferedCheck {
  public BlockBreakAttributeSignatureLockCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockBreakAttributeSignatureLock"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockBreakEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.itemAttackSpeedBonus()>1.1D && e.itemMovementSpeedBonus()>0.1D && e.intervalNanos()<80_000_000L;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"attribute signature lock",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
