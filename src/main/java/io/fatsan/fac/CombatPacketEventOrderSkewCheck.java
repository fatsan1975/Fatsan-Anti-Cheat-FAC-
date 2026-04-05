package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class CombatPacketEventOrderSkewCheck extends AbstractBufferedCheck {
  public CombatPacketEventOrderSkewCheck(int limit) { super(limit); }
  @Override public String name() { return "CombatPacketEventOrderSkew"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof CombatHitEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = e.intervalNanos()<70_000_000L && !e.onGround() && e.fallDistance()<0.01F;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "packet event order skew", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
