package io.fatsan.fac.check;

import io.fatsan.fac.check.support.FixedStepWindowTracker;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;

public final class CombatIntervalStepCheck extends AbstractBufferedCheck {
  private final FixedStepWindowTracker tracker = new FixedStepWindowTracker();

  public CombatIntervalStepCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "CombatIntervalStep";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) return CheckResult.clean(name(), category());
    if (hit.intervalNanos() == Long.MAX_VALUE) return CheckResult.clean(name(), category());

    long currentMillis = hit.intervalNanos() / 1_000_000L;
    int streak = tracker.record(hit.playerId(), currentMillis, 2L, 1L, 89L);
    if (streak > 0) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(), "CombatIntervalStep anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
      }
    } else {
      coolDown(hit.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
