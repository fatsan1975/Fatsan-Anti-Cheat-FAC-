package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class BlockBreakCustomContextBurstCheck extends AbstractBufferedCheck {
  public BlockBreakCustomContextBurstCheck(int limit){super(limit);} @Override public String name(){return "BlockBreakCustomContextBurst";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockBreakEventSignal b) || b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=b.customItemContext() && b.intervalNanos()/1_000_000L<45L; if(t){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"custom item context burst",Math.min(1D,bf/7D),true);} else coolDown(b.playerId()); return CheckResult.clean(name(),category()); }
}
