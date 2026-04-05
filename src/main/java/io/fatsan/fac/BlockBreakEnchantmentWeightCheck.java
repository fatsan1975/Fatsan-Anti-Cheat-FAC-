package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class BlockBreakEnchantmentWeightCheck extends AbstractBufferedCheck {
  public BlockBreakEnchantmentWeightCheck(int limit){super(limit);} @Override public String name(){return "BlockBreakEnchantmentWeight";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockBreakEventSignal b) || b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=b.enchantWeight()>12 && b.intervalNanos()/1_000_000L<35L; if(t){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"enchant weight boosted break",Math.min(1D,bf/7D),false);} else coolDown(b.playerId()); return CheckResult.clean(name(),category()); }
}
