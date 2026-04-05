package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.InventoryClickEventSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects automated item consumption — using food, potions, or totems at
 * machine precision through inventory interaction patterns.
 *
 * <p><b>Cheat behaviour:</b> "AutoConsume" and "AutoEat" cheats trigger item
 * use (right-click) at the exact moment the health/hunger bar drops below a
 * threshold.  When combined with hotbar swapping (which appears in
 * {@code InventoryClickEventSignal} as {@code offhandSwap}), the pattern is:
 * rapid successive inventory clicks + offhand swap, all within a very short
 * interval.
 *
 * <p><b>Detection:</b> Flag when:
 * <ul>
 *   <li>{@code offhandSwap == true} (player is swapping offhand item, common
 *       for totem-cycling cheats)</li>
 *   <li>{@code intervalNanos < threshold} (excessively fast swap)</li>
 * </ul>
 *
 * <p>This complements {@code AutoTotemCheck} which uses a different approach
 * (movement-coupled inventory click detection).
 *
 * <p>Buffer of 4 — legitimate "F" key swaps happen quickly but not sub-20ms.
 */
public final class AutoConsumeCheck extends AbstractBufferedCheck {

  private static final long MIN_SWAP_INTERVAL_NS = 20_000_000L; // 20ms

  public AutoConsumeCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "AutoConsume";
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

    if (!click.offhandSwap()) {
      coolDown(click.playerId());
      return CheckResult.clean(name(), category());
    }

    if (click.intervalNanos() == Long.MAX_VALUE || click.intervalNanos() <= 0) {
      return CheckResult.clean(name(), category());
    }

    if (click.intervalNanos() < MIN_SWAP_INTERVAL_NS) {
      int buf = incrementBuffer(click.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "AutoConsume: offhand swap interval=" + (click.intervalNanos() / 1_000_000)
                + "ms buf=" + buf,
            Math.min(0.85, buf / 5.0),
            false);
      }
    } else {
      coolDown(click.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
