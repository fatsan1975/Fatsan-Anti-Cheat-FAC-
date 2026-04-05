package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockBreakPerfectCadenceLockCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  private final Map<String, Integer> streak = new ConcurrentHashMap<>();
  public BlockBreakPerfectCadenceLockCheck(int limit){super(limit);} 
  @Override public String name(){return "BlockBreakPerfectCadenceLock";}
  @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof BlockBreakEventSignal b) || b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    long ms=b.intervalNanos()/1_000_000L; Long prev=last.put(b.playerId(),ms); if(prev==null) return CheckResult.clean(name(),category());
    boolean lock=Math.abs(ms-prev)<=1L && ms>=65L && ms<=120L;
    int s=lock?streak.getOrDefault(b.playerId(),0)+1:0; streak.put(b.playerId(),s);
    if(s>=8){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"perfect break cadence lock",Math.min(1D,bf/7D),false);} else coolDown(b.playerId());
    return CheckResult.clean(name(),category());
  }
}
