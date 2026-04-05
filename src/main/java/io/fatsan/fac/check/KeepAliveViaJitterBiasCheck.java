package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class KeepAliveViaJitterBiasCheck extends AbstractBufferedCheck {
  public KeepAliveViaJitterBiasCheck(int limit) { super(limit); }
  @Override public String name() { return "KeepAliveViaJitterBias"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof KeepAliveSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.pingMillis()>380 && e.pingMillis()<1000;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "via jitter bias ping band", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
