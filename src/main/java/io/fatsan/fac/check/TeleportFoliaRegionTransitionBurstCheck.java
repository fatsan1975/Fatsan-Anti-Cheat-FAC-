package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportFoliaRegionTransitionBurstCheck extends AbstractBufferedCheck {
  private final Map<String, Long> lastTeleport = new ConcurrentHashMap<>();
  public TeleportFoliaRegionTransitionBurstCheck(int limit) { super(limit); }
  @Override public String name() { return "TeleportFoliaRegionTransitionBurst"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof TeleportSignal t)) return CheckResult.clean(name(), category());
    long prev = lastTeleport.getOrDefault(t.playerId(), Long.MIN_VALUE);
    lastTeleport.put(t.playerId(), t.nanoTime());
    if (prev == Long.MIN_VALUE) return CheckResult.clean(name(), category());
    long delta = t.nanoTime() - prev;
    boolean trigger = delta > 0L && delta < 140_000_000L;
    if(trigger){ int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"folia region transition burst",Math.min(1D,b/7D),true);} else coolDown(t.playerId());
    return CheckResult.clean(name(), category());
  }
}
