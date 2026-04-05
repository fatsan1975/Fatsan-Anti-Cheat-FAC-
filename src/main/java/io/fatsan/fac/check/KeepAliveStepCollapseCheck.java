package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class KeepAliveStepCollapseCheck extends AbstractBufferedCheck {
  private final Map<String,Long> last=new ConcurrentHashMap<>();
  public KeepAliveStepCollapseCheck(int limit){super(limit);} @Override public String name(){return "KeepAliveStepCollapse";} @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof KeepAliveSignal k)) return CheckResult.clean(name(),category()); Long p=last.put(k.playerId(),k.pingMillis());
    boolean t=p!=null && Math.abs(k.pingMillis()-p)<=1L; if(t){int b=incrementBuffer(k.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"keepalive step collapse",Math.min(1D,b/7D),false);} else coolDown(k.playerId()); return CheckResult.clean(name(),category());}
}
