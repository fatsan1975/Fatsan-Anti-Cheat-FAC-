package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class BlockBreakEfficiencySpoofCheck extends AbstractBufferedCheck {
  public BlockBreakEfficiencySpoofCheck(int limit){super(limit);} @Override public String name(){return "BlockBreakEfficiencySpoof";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockBreakEventSignal b)||b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=b.efficiencyLevel()>=5 && b.intervalNanos()/1_000_000L<20L; if(t){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"efficiency spoof-like break speed",Math.min(1D,bf/7D),false);} else coolDown(b.playerId()); return CheckResult.clean(name(),category());}
}
