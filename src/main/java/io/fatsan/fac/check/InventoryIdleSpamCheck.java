package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.InventoryClickEventSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryIdleSpamCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> streak = new ConcurrentHashMap<>();
  public InventoryIdleSpamCheck(int limit) { super(limit); }
  @Override public String name() { return "InventoryIdleSpam"; }
  @Override public CheckCategory category() { return CheckCategory.INVENTORY; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof InventoryClickEventSignal i) || i.intervalNanos() == Long.MAX_VALUE) return CheckResult.clean(name(), category());
    long ms = i.intervalNanos() / 1_000_000L;
    if (!i.movingFast() && ms > 0 && ms < 45L) {
      int st = streak.getOrDefault(i.playerId(), 0) + 1; streak.put(i.playerId(), st);
      if (st >= 6) {
        int buf = incrementBuffer(i.playerId());
        if (overLimit(buf)) return new CheckResult(true, name(), category(), "Idle inventory click spam burst", Math.min(1.0D, buf / 8.0D), false);
      }
    } else { streak.put(i.playerId(), 0); coolDown(i.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
