package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class KeepAliveHighLowFlipCheck extends AbstractBufferedCheck {
  private final Map<String,Long> last=new ConcurrentHashMap<>();
  public KeepAliveHighLowFlipCheck(int limit){super(limit);} @Override public String name(){return "KeepAliveHighLowFlip";} @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof KeepAliveSignal k)) return CheckResult.clean(name(),category()); Long p=last.put(k.playerId(),k.pingMillis()); if(p==null) return CheckResult.clean(name(),category());
    boolean t=(p<90L&&k.pingMillis()>220L)||(p>220L&&k.pingMillis()<90L); if(t){int b=incrementBuffer(k.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"keepalive high/low flip",Math.min(1D,b/7D),false);} else coolDown(k.playerId()); return CheckResult.clean(name(),category());}
}
