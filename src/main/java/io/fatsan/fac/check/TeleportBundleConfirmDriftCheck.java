package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportBundleConfirmDriftCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public TeleportBundleConfirmDriftCheck(int limit) { super(limit); }
  @Override public String name() { return "TeleportBundleConfirmDrift"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof TeleportSignal t)) return CheckResult.clean(name(), category());
    long prev = last.getOrDefault(t.playerId(), Long.MIN_VALUE);
    last.put(t.playerId(), t.nanoTime());
    if(prev == Long.MIN_VALUE) return CheckResult.clean(name(), category());
    long delta = t.nanoTime() - prev;
    boolean trigger = delta > 0L && delta < 85_000_000L;
    if(trigger){ int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"teleport bundle confirm drift",Math.min(1D,b/7D),true);}
    else coolDown(t.playerId());
    return CheckResult.clean(name(), category());
  }
}
