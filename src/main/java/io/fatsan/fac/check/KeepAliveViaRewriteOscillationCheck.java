package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class KeepAliveViaRewriteOscillationCheck extends AbstractBufferedCheck {
  public KeepAliveViaRewriteOscillationCheck(int limit) { super(limit); }
  @Override public String name() { return "KeepAliveViaRewriteOscillation"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof KeepAliveSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.pingMillis()>250 && e.pingMillis()<290;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"keepalive via rewrite oscillation",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
