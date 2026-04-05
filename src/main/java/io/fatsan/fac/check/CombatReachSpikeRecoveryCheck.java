package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class CombatReachSpikeRecoveryCheck extends AbstractBufferedCheck {
  private final Map<String,Double> last=new ConcurrentHashMap<>();
  public CombatReachSpikeRecoveryCheck(int limit){super(limit);} @Override public String name(){return "CombatReachSpikeRecovery";} @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof CombatHitEvent h)) return CheckResult.clean(name(),category()); Double p=last.put(h.playerId(),h.reachDistance()); if(p==null) return CheckResult.clean(name(),category());
    boolean t=(p>3.45D && h.reachDistance()<2.75D)||(p<2.75D && h.reachDistance()>3.45D); if(t){int b=incrementBuffer(h.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"reach spike/recovery pattern",Math.min(1D,b/7D),false);} else coolDown(h.playerId()); return CheckResult.clean(name(),category());}
}
