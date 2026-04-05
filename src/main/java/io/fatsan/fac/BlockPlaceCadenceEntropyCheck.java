package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class BlockPlaceCadenceEntropyCheck extends AbstractBufferedCheck {
  private final Map<String,Long> last=new ConcurrentHashMap<>();
  public BlockPlaceCadenceEntropyCheck(int limit){super(limit);} @Override public String name(){return "BlockPlaceCadenceEntropy";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockPlaceEventSignal p)||p.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category()); long ms=p.intervalNanos()/1_000_000L; Long pv=last.put(p.playerId(),ms);
    boolean t=pv!=null && Math.abs(ms-pv)<=1L && ms<75L; if(t){int b=incrementBuffer(p.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"place cadence entropy collapse",Math.min(1D,b/7D),false);} else coolDown(p.playerId()); return CheckResult.clean(name(),category());}
}
