package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakUnbreakableSpeedCouplingCheck extends AbstractBufferedCheck {
  public BlockBreakUnbreakableSpeedCouplingCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockBreakUnbreakableSpeedCoupling"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockBreakEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.customItemContext() && e.itemAttackSpeedBonus()>0.6D && e.intervalNanos()<68_000_000L;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"unbreakable speed coupling",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
