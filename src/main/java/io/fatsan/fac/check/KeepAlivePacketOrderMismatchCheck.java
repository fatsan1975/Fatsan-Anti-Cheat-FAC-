package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class KeepAlivePacketOrderMismatchCheck extends AbstractBufferedCheck {
  public KeepAlivePacketOrderMismatchCheck(int limit) { super(limit); }
  @Override public String name() { return "KeepAlivePacketOrderMismatch"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof KeepAliveSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.pingMillis()==0 || e.pingMillis()>1200;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "keepalive packet order mismatch", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
