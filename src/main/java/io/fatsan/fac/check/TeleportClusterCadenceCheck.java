package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class TeleportClusterCadenceCheck extends AbstractBufferedCheck {
  private final Map<String,Long> last=new ConcurrentHashMap<>(); private final Map<String,Integer> streak=new ConcurrentHashMap<>();
  public TeleportClusterCadenceCheck(int limit){super(limit);} @Override public String name(){return "TeleportClusterCadence";} @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof TeleportSignal t)) return CheckResult.clean(name(),category()); Long p=last.put(t.playerId(),t.nanoTime()); if(p==null) return CheckResult.clean(name(),category());
    long ms=(t.nanoTime()-p)/1_000_000L; int s=(ms>100L&&ms<450L)?streak.getOrDefault(t.playerId(),0)+1:0; streak.put(t.playerId(),s); if(s>=6){int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"teleport cluster cadence",Math.min(1D,b/7D),false);} else coolDown(t.playerId()); return CheckResult.clean(name(),category());}
}
