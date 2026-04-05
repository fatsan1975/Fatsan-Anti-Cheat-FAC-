package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ScaffoldSprintPlaceCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> streak = new ConcurrentHashMap<>();
  public ScaffoldSprintPlaceCheck(int limit) { super(limit); }
  @Override public String name() { return "ScaffoldSprintPlace"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockPlaceEventSignal p) || p.intervalNanos() == Long.MAX_VALUE) return CheckResult.clean(name(), category());
    long ms = p.intervalNanos() / 1_000_000L;
    if (p.sprinting() && p.horizontalSpeed() > 0.26D && ms > 0 && ms < 90L) {
      int st = streak.getOrDefault(p.playerId(), 0) + 1; streak.put(p.playerId(), st);
      if (st >= 4) {
        int buf = incrementBuffer(p.playerId());
        if (overLimit(buf)) return new CheckResult(true, name(), category(), "Sprint scaffold-like place burst while moving", Math.min(1.0D, buf / 8.0D), true);
      }
    } else { streak.put(p.playerId(), 0); coolDown(p.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
