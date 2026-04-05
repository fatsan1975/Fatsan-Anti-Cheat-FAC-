package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.InventoryClickEventSignal;
import io.fatsan.fac.model.NormalizedEvent;

public final class InventoryMoveCheck extends AbstractBufferedCheck {
  public InventoryMoveCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "InventoryMove";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.INVENTORY;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof InventoryClickEventSignal click)) {
      return CheckResult.clean(name(), category());
    }

    long intervalMs = click.intervalNanos() / 1_000_000L;
    if (click.movingFast() && intervalMs < 80) {
      int buf = incrementBuffer(click.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Inventory actions during fast movement",
            Math.min(1.0D, buf / 8.0D),
            true);
      }
    } else {
      coolDown(click.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
