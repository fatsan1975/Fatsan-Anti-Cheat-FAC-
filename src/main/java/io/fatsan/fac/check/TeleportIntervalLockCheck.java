package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportIntervalLockCheck extends AbstractBufferedCheck {
  private final Map<String,Long> last=new ConcurrentHashMap<>();
  private final Map<String,Integer> streak=new ConcurrentHashMap<>();
  public TeleportIntervalLockCheck(int limit){super(limit);} 
  @Override public String name(){return "TeleportIntervalLock";}
  @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){
    if(!(event instanceof TeleportSignal t)) return CheckResult.clean(name(),category());
    Long prev=last.put(t.playerId(),t.nanoTime()); if(prev==null) return CheckResult.clean(name(),category());
    long ms=(t.nanoTime()-prev)/1_000_000L;
    boolean lock=ms>120L && ms<1500L && (ms%50L)<=2L;
    int s=lock?streak.getOrDefault(t.playerId(),0)+1:0; streak.put(t.playerId(),s);
    if(s>=5){int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"teleport interval lock",Math.min(1D,b/7D),false);} else coolDown(t.playerId());
    return CheckResult.clean(name(),category());
  }
}
