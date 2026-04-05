package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class TrafficLatencySurfaceCheck extends AbstractBufferedCheck {
  public TrafficLatencySurfaceCheck(int limit){super(limit);} @Override public String name(){return "TrafficLatencySurface";} @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof TrafficSignal t)) return CheckResult.clean(name(),category());
    boolean tr=t.eventsPerSecond()>700 && t.dropped()>=7; if(tr){int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"traffic latency surface anomaly",Math.min(1D,b/7D),true);} else coolDown(t.playerId()); return CheckResult.clean(name(),category()); }
}
