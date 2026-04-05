package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockBreakEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

public final class FastBreakCheck extends AbstractBufferedCheck {
  public FastBreakCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "FastBreak";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockBreakEventSignal blockBreak)
        || blockBreak.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }
    long intervalMs = blockBreak.intervalNanos() / 1_000_000L;
    if (intervalMs > 0 && intervalMs < 70) {
      int buf = incrementBuffer(blockBreak.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(), "Break interval too low", Math.min(1.0D, buf / 8.0D), true);
      }
    } else {
      coolDown(blockBreak.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
