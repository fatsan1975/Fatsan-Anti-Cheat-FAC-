package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class TrafficViaWindowAnomalyCheck extends AbstractBufferedCheck {
  public TrafficViaWindowAnomalyCheck(int limit) { super(limit); }
  @Override public String name() { return "TrafficViaWindowAnomaly"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof TrafficSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.eventsPerSecond()>980 && e.dropped()>3;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"traffic via window anomaly",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
