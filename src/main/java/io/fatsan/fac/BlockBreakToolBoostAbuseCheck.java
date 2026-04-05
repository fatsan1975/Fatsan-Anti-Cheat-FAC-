package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakToolBoostAbuseCheck extends AbstractBufferedCheck {
  public BlockBreakToolBoostAbuseCheck(int limit){super(limit);} 
  @Override public String name(){return "BlockBreakToolBoostAbuse";}
  @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof BlockBreakEventSignal b) || b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    long ms=b.intervalNanos()/1_000_000L;
    boolean trigger=ms<55L && b.efficiencyLevel()<=1 && b.hasteAmplifier()<0;
    if(trigger){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"fast break without tool boost context",Math.min(1D,bf/7D),true);} else coolDown(b.playerId());
    return CheckResult.clean(name(),category());
  }
}
