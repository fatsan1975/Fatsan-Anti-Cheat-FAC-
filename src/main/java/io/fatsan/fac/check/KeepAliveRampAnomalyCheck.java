package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class KeepAliveRampAnomalyCheck extends AbstractBufferedCheck {
  private final Map<String,Long> last=new ConcurrentHashMap<>(); private final Map<String,Integer> streak=new ConcurrentHashMap<>();
  public KeepAliveRampAnomalyCheck(int limit){super(limit);} @Override public String name(){return "KeepAliveRampAnomaly";} @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof KeepAliveSignal k)) return CheckResult.clean(name(),category()); Long p=last.put(k.playerId(),k.pingMillis()); if(p==null) return CheckResult.clean(name(),category());
    int s=((k.pingMillis()-p)>=25L)?streak.getOrDefault(k.playerId(),0)+1:0; streak.put(k.playerId(),s); if(s>=5){int b=incrementBuffer(k.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"keepalive ramp anomaly",Math.min(1D,b/7D),false);} else coolDown(k.playerId()); return CheckResult.clean(name(),category());}
}
