package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class BlockPlaceProtocolSprintSkewCheck extends AbstractBufferedCheck {
  public BlockPlaceProtocolSprintSkewCheck(int limit) { super(limit); }
  @Override public String name() { return "BlockPlaceProtocolSprintSkew"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof BlockPlaceEventSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.sprinting() && e.horizontalSpeed()>0.45D && e.intervalNanos()<80_000_000L;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "block place sprint protocol skew", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
