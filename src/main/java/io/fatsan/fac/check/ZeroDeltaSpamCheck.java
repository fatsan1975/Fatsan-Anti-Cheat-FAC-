package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ZeroDeltaSpamCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> streak = new ConcurrentHashMap<>();
  public ZeroDeltaSpamCheck(int limit) { super(limit); }
  @Override public String name() { return "ZeroDeltaSpam"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent m) || m.intervalNanos() == Long.MAX_VALUE) return CheckResult.clean(name(), category());
    boolean spam = m.deltaXZ() < 0.0001D && Math.abs(m.deltaY()) < 0.0001D && m.intervalNanos() < 2_000_000L;
    if (spam) {
      int st = streak.getOrDefault(m.playerId(), 0) + 1; streak.put(m.playerId(), st);
      if (st >= 8) {
        int buf = incrementBuffer(m.playerId());
        if (overLimit(buf)) return new CheckResult(true, name(), category(), "Abnormal zero-delta micro-interval movement spam", Math.min(1.0D, buf / 8.0D), false);
      }
    } else { streak.put(m.playerId(), 0); coolDown(m.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
