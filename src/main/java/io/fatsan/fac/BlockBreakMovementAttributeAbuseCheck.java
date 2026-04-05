package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class BlockBreakMovementAttributeAbuseCheck extends AbstractBufferedCheck {
  public BlockBreakMovementAttributeAbuseCheck(int limit){super(limit);} @Override public String name(){return "BlockBreakMovementAttributeAbuse";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockBreakEventSignal b)||b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=b.itemMovementSpeedBonus()>0.12D && b.intervalNanos()/1_000_000L<65L; if(t){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"movement-attribute assisted break",Math.min(1D,bf/7D),true);} else coolDown(b.playerId()); return CheckResult.clean(name(),category()); }
}
