package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class TrafficViaBandwidthAnomalyCheck extends AbstractBufferedCheck {
  public TrafficViaBandwidthAnomalyCheck(int limit) { super(limit); }
  @Override public String name() { return "TrafficViaBandwidthAnomaly"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof TrafficSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.eventsPerSecond()>900 && e.dropped()>5;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "via bandwidth anomaly", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
