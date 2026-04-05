package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.InventoryClickEventSignal;
import io.fatsan.fac.model.PlayerStateEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * AutoArmor Detection (81)
 * Otomatik zırh giydirme tespiti
 */
public class AutoArmorCheck implements Check {
  private final int bufferLimit;
  private final Map<String, ArmorClickData> playerData = new ConcurrentHashMap<>();

  public AutoArmorCheck(int bufferLimit) {
    this.bufferLimit = bufferLimit;
  }

  @Override
  public String name() {
    return "AutoArmor";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.INVENTORY;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (event instanceof InventoryClickEventSignal click) {
      return evaluateClick(click);
    } else if (event instanceof PlayerStateEvent state) {
      return evaluateMovement(state);
    }
    return CheckResult.clean(name(), category());
  }

  private CheckResult evaluateClick(InventoryClickEventSignal click) {
    ArmorClickData data = playerData.computeIfAbsent(click.playerId(), k -> new ArmorClickData());

    long now = click.nanoTime();
    long interval = click.intervalNanos();

    data.clickCount++;
    data.intervals.add(interval);

    if (data.intervals.size() > 5) {
      data.intervals.remove(0);
    }

    if (data.firstClickTime == 0) {
      data.firstClickTime = now;
    }

    // Check for consistent timing (AutoArmor signature)
    if (data.intervals.size() >= 3) {
      long avg = data.intervals.stream().mapToLong(Long::longValue).sum() / data.intervals.size();
      long variance = data.intervals.stream()
          .mapToLong(i -> Math.abs(i - avg))
          .sum() / data.intervals.size();

      // Low variance = AutoArmor
      if (variance < 10_000_000 && avg < 50_000_000) { // 50ms
        data.suspicion++;
        if (data.suspicion > bufferLimit) {
          return new CheckResult(true, name(), category(), "AutoArmor detected (variance=" + variance + "ns)", 0.75, false);
        }
      }
    }

    // Offhand swap in rapid succession is suspicious (AutoArmor hotkey)
    if (click.offhandSwap()) {
      data.hotkeyUses++;
      if (data.hotkeyUses > 3 && data.clickCount > 3) {
        long timeSpan = now - data.firstClickTime;
        if (timeSpan < 100_000_000) { // 100ms
          return new CheckResult(true, name(), category(), "AutoArmor instant swap detected", 0.8, false);
        }
      }
    }

    return CheckResult.clean(name(), category());
  }

  private CheckResult evaluateMovement(PlayerStateEvent state) {
    ArmorClickData data = playerData.get(state.playerId());
    if (data == null) {
      return CheckResult.clean(name(), category());
    }

    // AutoArmor works while moving
    if (state.deltaXZ() > 0.1 && data.clickCount > 0) {
      data.movingClicks++;
      if (data.movingClicks > bufferLimit) {
        return new CheckResult(true, name(), category(), "AutoArmor while moving detected", 0.7, false);
      }
    }

    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    playerData.remove(playerId);
  }

  private static class ArmorClickData {
    volatile int clickCount;
    volatile int hotkeyUses;
    volatile int movingClicks;
    volatile int suspicion;
    volatile long firstClickTime;
    final List<Long> intervals = new ArrayList<>();
  }
}
