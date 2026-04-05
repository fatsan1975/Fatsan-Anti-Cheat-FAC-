package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.TrafficSignal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TrafficFloodRampCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> streak = new ConcurrentHashMap<>();
  public TrafficFloodRampCheck(int limit) { super(limit); }
  @Override public String name() { return "TrafficFloodRamp"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof TrafficSignal t)) return CheckResult.clean(name(), category());
    if (t.eventsPerSecond() > 900 && t.dropped() > 0) {
      int st = streak.getOrDefault(t.playerId(), 0) + 1; streak.put(t.playerId(), st);
      if (st >= 2) {
        int buf = incrementBuffer(t.playerId());
        if (overLimit(buf)) return new CheckResult(true, name(), category(), "Escalating traffic flood ramp with dropped events", Math.min(1.0D, buf / 8.0D), true);
      }
    } else { streak.put(t.playerId(), 0); coolDown(t.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
