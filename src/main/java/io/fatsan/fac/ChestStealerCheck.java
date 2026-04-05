package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.InventoryClickEventSignal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * ChestStealer Detection (82)
 * Otomatik sandık yağmalama tespiti
 */
public class ChestStealerCheck implements Check {
  private final int bufferLimit;
  private final Map<String, StealerData> playerData = new ConcurrentHashMap<>();

  public ChestStealerCheck(int bufferLimit) {
    this.bufferLimit = bufferLimit;
  }

  @Override
  public String name() {
    return "ChestStealer";
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

    StealerData data = playerData.computeIfAbsent(click.playerId(), k -> new StealerData());

    long now = click.nanoTime();
    long interval = click.intervalNanos();

    data.containerClicks++;

    if (data.firstClickTime == 0) {
      data.firstClickTime = now;
    }

    data.intervals.add(interval);

    if (data.intervals.size() > 10) {
      data.intervals.remove(0);
    }

    // Check for inhuman click speed
    if (interval < 15_000_000) { // 15ms
      data.fastClicks++;
      if (data.fastClicks > 3) {
        return new CheckResult(true, name(), category(), "ChestStealer inhuman speed detected", 0.85, false);
      }
    }

    // Check pattern after several clicks
    if (data.containerClicks >= 5 && data.intervals.size() >= 5) {
      // Calculate consistency
      long avg = data.intervals.stream().mapToLong(Long::longValue).sum() / data.intervals.size();
      long variance = data.intervals.stream()
          .mapToLong(i -> Math.abs(i - avg))
          .sum() / data.intervals.size();

      // ChestStealer has very consistent timing
      if (variance < 5_000_000 && avg < 30_000_000) {
        data.suspicion++;
        if (data.suspicion > bufferLimit) {
          return new CheckResult(true, name(), category(), "ChestStealer consistent timing detected", 0.8, false);
        }
      }
    }

    // Check total time for full steal (27 slots worth of clicks in 500ms)
    long totalTime = now - data.firstClickTime;
    if (data.containerClicks >= 27 && totalTime < 500_000_000) {
      return new CheckResult(true, name(), category(), "ChestStealer full inventory steal detected", 0.9, false);
    }

    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    playerData.remove(playerId);
  }

  private static class StealerData {
    volatile int containerClicks;
    volatile int fastClicks;
    volatile int suspicion;
    volatile long firstClickTime;
    final List<Long> intervals = new ArrayList<>();
  }
}
