package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportViaConfirmSkewCheck extends AbstractBufferedCheck {
  private final Map<String, Long> lastTeleport = new ConcurrentHashMap<>();
  public TeleportViaConfirmSkewCheck(int limit) { super(limit); }
  @Override public String name() { return "TeleportViaConfirmSkew"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof TeleportSignal t)) return CheckResult.clean(name(), category());
    long prev = lastTeleport.getOrDefault(t.playerId(), Long.MIN_VALUE);
    lastTeleport.put(t.playerId(), t.nanoTime());
    if (prev == Long.MIN_VALUE) return CheckResult.clean(name(), category());
    boolean trigger = t.nanoTime() - prev < 90_000_000L;
    if(trigger){ int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"teleport confirm skew",Math.min(1D,b/7D),true);} else coolDown(t.playerId());
    return CheckResult.clean(name(), category());
  }
}
