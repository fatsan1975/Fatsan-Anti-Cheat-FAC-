package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportRegionIoFusionCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public TeleportRegionIoFusionCheck(int limit) { super(limit); }
  @Override public String name() { return "TeleportRegionIoFusion"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof TeleportSignal t)) return CheckResult.clean(name(), category());
    long prev = last.getOrDefault(t.playerId(), Long.MIN_VALUE);
    last.put(t.playerId(), t.nanoTime());
    if(prev == Long.MIN_VALUE) return CheckResult.clean(name(), category());
    long delta = t.nanoTime() - prev;
    boolean trigger = delta > 0L && delta < 135_000_000L;
    if(trigger){ int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"teleport region io fusion",Math.min(1D,b/7D),true);}
    else coolDown(t.playerId());
    return CheckResult.clean(name(), category());
  }
}
