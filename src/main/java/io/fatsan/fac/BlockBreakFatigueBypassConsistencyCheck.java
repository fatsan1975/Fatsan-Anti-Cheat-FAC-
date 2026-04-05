package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class BlockBreakFatigueBypassConsistencyCheck extends AbstractBufferedCheck {
  public BlockBreakFatigueBypassConsistencyCheck(int limit){super(limit);} @Override public String name(){return "BlockBreakFatigueBypassConsistency";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockBreakEventSignal b)||b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=b.miningFatigueAmplifier()>=0 && b.intervalNanos()/1_000_000L<90L; if(t){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"fatigue bypass consistency",Math.min(1D,bf/7D),false);} else coolDown(b.playerId()); return CheckResult.clean(name(),category()); }
}
