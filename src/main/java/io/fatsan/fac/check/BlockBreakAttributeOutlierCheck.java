package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class BlockBreakAttributeOutlierCheck extends AbstractBufferedCheck {
  public BlockBreakAttributeOutlierCheck(int limit){super(limit);} @Override public String name(){return "BlockBreakAttributeOutlier";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockBreakEventSignal b)||b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=(b.attackSpeedAttribute()>8.0D||b.movementSpeedAttribute()>0.4D) && b.intervalNanos()/1_000_000L<70L; if(t){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"attribute outlier assisted break",Math.min(1D,bf/7D),true);} else coolDown(b.playerId()); return CheckResult.clean(name(),category());}
}
