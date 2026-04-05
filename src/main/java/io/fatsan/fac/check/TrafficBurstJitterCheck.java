package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.TrafficSignal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TrafficBurstJitterCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> burstStreak = new ConcurrentHashMap<>();

  public TrafficBurstJitterCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "TrafficBurstJitter";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof TrafficSignal traffic)) return CheckResult.clean(name(), category());

    boolean burst = traffic.eventsPerSecond() > 900 && traffic.dropped() >= 3;
    int streak = burst ? burstStreak.getOrDefault(traffic.playerId(), 0) + 1 : 0;
    burstStreak.put(traffic.playerId(), streak);

    if (streak >= 4) {
      int buf = incrementBuffer(traffic.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(), "packet burst+jitter coupling", Math.min(1.0D, buf / 6.0D), false);
      }
    } else {
      coolDown(traffic.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
