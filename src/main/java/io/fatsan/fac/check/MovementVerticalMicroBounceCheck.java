package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class MovementVerticalMicroBounceCheck extends AbstractBufferedCheck {
  private final Map<String,Double> last=new ConcurrentHashMap<>();
  public MovementVerticalMicroBounceCheck(int limit){super(limit);} @Override public String name(){return "MovementVerticalMicroBounce";} @Override public CheckCategory category(){return CheckCategory.MOVEMENT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof MovementEvent m)||m.onGround()) return CheckResult.clean(name(),category()); Double p=last.put(m.playerId(),m.deltaY());
    boolean t=p!=null && Math.abs(p)>0.02D && Math.abs(m.deltaY())>0.02D && Math.signum(p)!=Math.signum(m.deltaY()) && Math.abs(p-m.deltaY())<0.01D; if(t){int b=incrementBuffer(m.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"micro-bounce oscillation",Math.min(1D,b/7D),false);} else coolDown(m.playerId()); return CheckResult.clean(name(),category());}
}
