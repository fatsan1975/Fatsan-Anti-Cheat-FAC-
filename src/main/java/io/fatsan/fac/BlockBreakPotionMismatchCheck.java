package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakPotionMismatchCheck extends AbstractBufferedCheck {
  public BlockBreakPotionMismatchCheck(int limit){super(limit);} 
  @Override public String name(){return "BlockBreakPotionMismatch";}
  @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof BlockBreakEventSignal b) || b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    long ms=b.intervalNanos()/1_000_000L;
    boolean trigger=b.miningFatigueAmplifier()>=0 && ms<70L;
    if(trigger){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"break speed ignores mining fatigue",Math.min(1D,bf/7D),false);} else coolDown(b.playerId());
    return CheckResult.clean(name(),category());
  }
}
