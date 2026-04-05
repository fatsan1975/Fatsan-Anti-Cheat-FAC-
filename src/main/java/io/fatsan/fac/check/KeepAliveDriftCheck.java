package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeepAliveDriftCheck extends AbstractBufferedCheck {
  private final Map<String, Long> lastPing = new ConcurrentHashMap<>();
  private final Map<String, Integer> driftStreak = new ConcurrentHashMap<>();

  public KeepAliveDriftCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "KeepAliveDrift";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof KeepAliveSignal keepAlive)) {
      return CheckResult.clean(name(), category());
    }

    Long previous = lastPing.put(keepAlive.playerId(), keepAlive.pingMillis());
    if (previous == null) {
      return CheckResult.clean(name(), category());
    }

    long diff = keepAlive.pingMillis() - previous;
    if (diff >= 70L) {
      int streak = driftStreak.getOrDefault(keepAlive.playerId(), 0) + 1;
      driftStreak.put(keepAlive.playerId(), streak);
      if (streak >= 4) {
        int buf = incrementBuffer(keepAlive.playerId());
        if (overLimit(buf)) {
          return new CheckResult(true, name(), category(), "Sustained keepalive ping drift escalation", Math.min(1.0D, buf / 8.0D), false);
        }
      }
    } else {
      driftStreak.put(keepAlive.playerId(), 0);
      coolDown(keepAlive.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
