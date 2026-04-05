package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class CombatViaVersionReachDesyncCheck extends AbstractBufferedCheck {
  public CombatViaVersionReachDesyncCheck(int limit) { super(limit); }
  @Override public String name() { return "CombatViaVersionReachDesync"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof CombatHitEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = e.reachDistance()>3.35D && e.intervalNanos()<140_000_000L;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "via reach desync pattern", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
