package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class TrafficPacketBundlePressureCheck extends AbstractBufferedCheck {
  public TrafficPacketBundlePressureCheck(int limit) { super(limit); }
  @Override public String name() { return "TrafficPacketBundlePressure"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof TrafficSignal e)) return CheckResult.clean(name(), category());
    boolean trigger = e.eventsPerSecond()>850 && e.dropped()>12;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"traffic packet bundle pressure",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
