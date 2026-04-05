package io.fatsan.fac.check;

import io.fatsan.fac.check.support.FixedStepWindowTracker;
import io.fatsan.fac.model.BlockBreakEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

public final class BlockBreakStepPatternCheck extends AbstractBufferedCheck {
  private final FixedStepWindowTracker tracker = new FixedStepWindowTracker();

  public BlockBreakStepPatternCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "BlockBreakStepPattern";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockBreakEventSignal b) || b.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    long millis = b.intervalNanos() / 1_000_000L;
    int streak = tracker.record(b.playerId(), millis, 2L, 1L, 119L);
    if (streak > 0) {
      int buf = incrementBuffer(b.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(), "Fixed-step block break rhythm pattern", Math.min(1.0D, buf / 8.0D), false);
      }
    } else {
      coolDown(b.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
