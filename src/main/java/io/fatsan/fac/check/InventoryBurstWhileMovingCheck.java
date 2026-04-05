package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.InventoryClickEventSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryBurstWhileMovingCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> burstStreak = new ConcurrentHashMap<>();

  public InventoryBurstWhileMovingCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "InventoryBurstWhileMoving";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.INVENTORY;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof InventoryClickEventSignal click) || click.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    long intervalMs = click.intervalNanos() / 1_000_000L;
    if (click.movingFast() && intervalMs > 0 && intervalMs < 80L) {
      int streak = burstStreak.getOrDefault(click.playerId(), 0) + 1;
      burstStreak.put(click.playerId(), streak);
      if (streak >= 4) {
        int buf = incrementBuffer(click.playerId());
        if (overLimit(buf)) {
          return new CheckResult(true, name(), category(), "Inventory click burst while moving quickly", Math.min(1.0D, buf / 8.0D), false);
        }
      }
    } else {
      burstStreak.put(click.playerId(), 0);
      coolDown(click.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
