package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class CombatPacketBundleOrderCheck extends AbstractBufferedCheck {
  public CombatPacketBundleOrderCheck(int limit) { super(limit); }
  @Override public String name() { return "CombatPacketBundleOrder"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof CombatHitEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = e.intervalNanos()<60_000_000L && e.gliding() && e.reachDistance()>2.9D;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"packet bundle order combat",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
