package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockBreakSpeedAttributeSpikeCheck extends AbstractBufferedCheck {
  public BlockBreakSpeedAttributeSpikeCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockBreakSpeedAttributeSpike"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockBreakEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.itemMovementSpeedBonus()>0.2D && e.intervalNanos()/1_000_000L<85L;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "movement speed spike while breaking", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
