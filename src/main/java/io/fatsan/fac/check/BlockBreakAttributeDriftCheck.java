package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class BlockBreakAttributeDriftCheck extends AbstractBufferedCheck {
  private final Map<String,Double> last=new ConcurrentHashMap<>();
  public BlockBreakAttributeDriftCheck(int limit){super(limit);} @Override public String name(){return "BlockBreakAttributeDrift";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockBreakEventSignal b)||b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    double cur=b.itemAttackSpeedBonus()+b.itemMovementSpeedBonus(); Double p=last.put(b.playerId(),cur); boolean t=p!=null && Math.abs(cur-p)>1.0D && b.intervalNanos()/1_000_000L<70L;
    if(t){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"item attribute drift burst",Math.min(1D,bf/7D),true);} else coolDown(b.playerId()); return CheckResult.clean(name(),category()); }
}
