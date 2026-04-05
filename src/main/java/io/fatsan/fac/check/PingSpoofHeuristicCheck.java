package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.NormalizedEvent;

public final class PingSpoofHeuristicCheck extends AbstractBufferedCheck {
  public PingSpoofHeuristicCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "PingSpoofHeuristic";
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

    long ping = keepAlive.pingMillis();
    if (ping < 0 || ping > 2_500) {
      int buf = incrementBuffer(keepAlive.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(), "Ping outlier behavior", Math.min(1.0D, buf / 6.0D), false);
      }
    } else {
      coolDown(keepAlive.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
