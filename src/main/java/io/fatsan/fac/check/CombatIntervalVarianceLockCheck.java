package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatIntervalVarianceLockCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  private final Map<String, Integer> streak = new ConcurrentHashMap<>();
  public CombatIntervalVarianceLockCheck(int limit){super(limit);} 
  @Override public String name(){return "CombatIntervalVarianceLock";}
  @Override public CheckCategory category(){return CheckCategory.COMBAT;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof CombatHitEvent h) || h.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    long ms=h.intervalNanos()/1_000_000L; Long prev=last.put(h.playerId(),ms);
    if(prev==null) return CheckResult.clean(name(),category());
    boolean lock=Math.abs(ms-prev)<=1L && ms>=55L && ms<=120L;
    int s=lock?streak.getOrDefault(h.playerId(),0)+1:0; streak.put(h.playerId(),s);
    if(s>=8){int b=incrementBuffer(h.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"combat interval variance lock",Math.min(1D,b/7D),false);} else coolDown(h.playerId());
    return CheckResult.clean(name(),category());
  }
}
