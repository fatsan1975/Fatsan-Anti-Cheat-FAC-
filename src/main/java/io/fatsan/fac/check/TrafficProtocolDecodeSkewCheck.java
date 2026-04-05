package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class TrafficProtocolDecodeSkewCheck extends AbstractBufferedCheck {
  public TrafficProtocolDecodeSkewCheck(int limit) { super(limit); }
  @Override public String name() { return "TrafficProtocolDecodeSkew"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof TrafficSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.eventsPerSecond()>1100 && e.dropped()>0;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "protocol decode skew", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
