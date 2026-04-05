package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class MovementFallResetCadenceCheck extends AbstractBufferedCheck {
  public MovementFallResetCadenceCheck(int limit){super(limit);} @Override public String name(){return "MovementFallResetCadence";} @Override public CheckCategory category(){return CheckCategory.MOVEMENT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof MovementEvent m)||m.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=!m.onGround() && m.fallDistance()==0.0F && m.deltaY()<0 && m.intervalNanos()<20_000_000L; if(t){int b=incrementBuffer(m.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"fall reset cadence",Math.min(1D,b/7D),true);} else coolDown(m.playerId()); return CheckResult.clean(name(),category());}
}
