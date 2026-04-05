package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class BlockBreakItemAttributeMismatchCheck extends AbstractBufferedCheck {
  public BlockBreakItemAttributeMismatchCheck(int limit){super(limit);} @Override public String name(){return "BlockBreakItemAttributeMismatch";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockBreakEventSignal b) || b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=b.itemAttackSpeedBonus()>0.8D && b.intervalNanos()/1_000_000L<65L && b.efficiencyLevel()<=1; if(t){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"item attribute mismatch fast-break",Math.min(1D,bf/7D),true);} else coolDown(b.playerId()); return CheckResult.clean(name(),category()); }
}
