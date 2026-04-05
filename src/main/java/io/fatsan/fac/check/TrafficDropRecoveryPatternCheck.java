package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class TrafficDropRecoveryPatternCheck extends AbstractBufferedCheck {
  private final Map<String,Integer> lastDrop=new ConcurrentHashMap<>();
  public TrafficDropRecoveryPatternCheck(int limit){super(limit);} @Override public String name(){return "TrafficDropRecoveryPattern";} @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof TrafficSignal t)) return CheckResult.clean(name(),category()); Integer p=lastDrop.put(t.playerId(),t.dropped()); if(p==null) return CheckResult.clean(name(),category());
    boolean tr=(p>0 && t.dropped()==0 && t.eventsPerSecond()>800)||(t.dropped()>10 && t.eventsPerSecond()>900); if(tr){int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"traffic drop recovery pattern",Math.min(1D,b/7D),true);} else coolDown(t.playerId()); return CheckResult.clean(name(),category());}
}
