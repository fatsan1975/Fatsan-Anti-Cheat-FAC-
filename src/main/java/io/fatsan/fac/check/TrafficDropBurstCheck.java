package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.TrafficSignal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TrafficDropBurstCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();

  public TrafficDropBurstCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "TrafficDropBurst";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof TrafficSignal traffic)) return CheckResult.clean(name(), category());
    boolean trigger = false;
    if (traffic.dropped() > 5 && traffic.eventsPerSecond() > 700) trigger = true;
    if (trigger) {
      int buf = incrementBuffer(traffic.playerId());
      if (overLimit(buf))
        return new CheckResult(
            true,
            name(),
            category(),
            "TrafficDropBurst anomaly pattern",
            Math.min(1.0D, buf / 8.0D),
            false);
    } else {
      coolDown(traffic.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
