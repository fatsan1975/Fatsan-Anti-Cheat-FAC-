package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TrafficCeilingOscillationCheck extends AbstractBufferedCheck {
  private final Map<String,Integer> sign=new ConcurrentHashMap<>();
  private final Map<String,Integer> flips=new ConcurrentHashMap<>();
  private final Map<String,Integer> lastEps=new ConcurrentHashMap<>();
  public TrafficCeilingOscillationCheck(int limit){super(limit);} 
  @Override public String name(){return "TrafficCeilingOscillation";}
  @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof TrafficSignal t)) return CheckResult.clean(name(),category());
    Integer prev=lastEps.put(t.playerId(),t.eventsPerSecond()); if(prev==null) return CheckResult.clean(name(),category());
    int d=t.eventsPerSecond()-prev; int cs=Integer.compare(d,0); int ps=sign.getOrDefault(t.playerId(),0);
    if(Math.abs(d)>=80 && ps!=0 && cs!=0 && ps!=cs) flips.put(t.playerId(),flips.getOrDefault(t.playerId(),0)+1);
    if(cs!=0) sign.put(t.playerId(),cs);
    if(flips.getOrDefault(t.playerId(),0)>=4 && t.eventsPerSecond()>650){int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"traffic ceiling oscillation",Math.min(1D,b/7D),true);} else coolDown(t.playerId());
    return CheckResult.clean(name(),category());
  }
}
