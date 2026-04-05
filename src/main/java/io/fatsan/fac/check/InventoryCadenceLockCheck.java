package io.fatsan.fac.check;

import io.fatsan.fac.check.support.FixedStepWindowTracker;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.InventoryClickEventSignal;
import io.fatsan.fac.model.NormalizedEvent;

public final class InventoryCadenceLockCheck extends AbstractBufferedCheck {
  private final FixedStepWindowTracker tracker = new FixedStepWindowTracker();

  public InventoryCadenceLockCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "InventoryCadenceLock";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.INVENTORY;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof InventoryClickEventSignal click)) return CheckResult.clean(name(), category());
    if (click.intervalNanos() == Long.MAX_VALUE) return CheckResult.clean(name(), category());

    long millis = click.intervalNanos() / 1_000_000L;
    int streak = tracker.record(click.playerId(), millis, 1L, 35L, 120L);
    if (streak >= 8 && !click.movingFast()) {
      int buf = incrementBuffer(click.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(), "inventory cadence lock", Math.min(1.0D, buf / 7.0D), false);
      }
    } else {
      coolDown(click.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
