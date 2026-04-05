package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.HotbarSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects hotbar slot-swap spam — changing hotbar slots faster than human input allows.
 *
 * <p><b>Cheat behaviour:</b> "AutoArmor", "AutoTotem", and some killaura cheats
 * rapidly swap hotbar items as part of their operation.  Vanilla hotbar changes
 * via keyboard require physical key presses; the human minimum reaction time
 * produces intervals ≥ 80–150ms between changes.  Script/macro-driven changes
 * can occur every 1–5ms.
 *
 * <p><b>Signal:</b> {@link HotbarSignal#intervalNanos()} is the time since the
 * previous slot change for this player.  Emitted in {@code BukkitSignalBridge}
 * on {@code PlayerItemHeldEvent}.
 *
 * <p><b>Threshold:</b> {@value #MIN_INTERVAL_MS}ms — below this, the slot
 * change is faster than any physical key press or scroll wheel input.
 *
 * <p>Buffer of 3 required to absorb occasional network reorder artifacts.
 */
public final class HotbarSwapSpamCheck extends AbstractBufferedCheck {

  private static final long MIN_INTERVAL_MS = 20L;
  private static final long MIN_INTERVAL_NS = MIN_INTERVAL_MS * 1_000_000L;

  public HotbarSwapSpamCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "HotbarSwapSpam";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.INVENTORY;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof HotbarSignal signal)) {
      return CheckResult.clean(name(), category());
    }

    if (signal.intervalNanos() == Long.MAX_VALUE || signal.intervalNanos() <= 0) {
      return CheckResult.clean(name(), category());
    }

    if (signal.intervalNanos() < MIN_INTERVAL_NS) {
      int buf = incrementBuffer(signal.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Hotbar swap spam: interval=" + (signal.intervalNanos() / 1_000_000)
                + "ms (" + signal.fromSlot() + "→" + signal.toSlot()
                + ") buf=" + buf,
            Math.min(0.9, buf / 5.0),
            false);
      }
    } else {
      coolDown(signal.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
