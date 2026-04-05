package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class KeepAliveAttributeSpoofPatternCheck extends AbstractBufferedCheck {
  public KeepAliveAttributeSpoofPatternCheck(int limit){super(limit);} @Override public String name(){return "KeepAliveAttributeSpoofPattern";} @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof KeepAliveSignal k)) return CheckResult.clean(name(),category());
    boolean t=k.pingMillis()>250L && (k.pingMillis()%10L)==0L; if(t){int b=incrementBuffer(k.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"keepalive spoof pattern",Math.min(1D,b/7D),false);} else coolDown(k.playerId()); return CheckResult.clean(name(),category()); }
}
