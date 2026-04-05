package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeepAliveConsistencyCheck extends AbstractBufferedCheck {
  private final Map<String, Long> lastPing = new ConcurrentHashMap<>();

  public KeepAliveConsistencyCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "KeepAliveConsistency";
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

    long diff = Math.abs(previous - keepAlive.pingMillis());
    if (keepAlive.pingMillis() > 600 && diff <= 1) {
      int buf = incrementBuffer(keepAlive.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Suspiciously frozen high ping pattern",
            Math.min(1.0D, buf / 8.0D),
            false);
      }
    } else {
      coolDown(keepAlive.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
