package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class TrafficPacketEventsSkewCheck extends AbstractBufferedCheck {
  public TrafficPacketEventsSkewCheck(int limit){super(limit);} @Override public String name(){return "TrafficPacketEventsSkew";} @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof TrafficSignal t)) return CheckResult.clean(name(),category());
    boolean tr=t.eventsPerSecond()>1000 && t.dropped()>0; if(tr){int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"packet-events skew burst",Math.min(1D,b/7D),true);} else coolDown(t.playerId()); return CheckResult.clean(name(),category()); }
}
