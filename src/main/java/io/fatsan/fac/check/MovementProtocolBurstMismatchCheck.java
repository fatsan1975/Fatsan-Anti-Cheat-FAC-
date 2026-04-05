package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class MovementProtocolBurstMismatchCheck extends AbstractBufferedCheck {
  public MovementProtocolBurstMismatchCheck(int limit){super(limit);} @Override public String name(){return "MovementProtocolBurstMismatch";} @Override public CheckCategory category(){return CheckCategory.MOVEMENT;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof MovementEvent m)||m.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    boolean t=m.intervalNanos()<5_000_000L && m.deltaXZ()>0.22D && Math.abs(m.deltaY())<0.02D; if(t){int b=incrementBuffer(m.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"movement protocol burst mismatch",Math.min(1D,b/7D),false);} else coolDown(m.playerId()); return CheckResult.clean(name(),category()); }
}
