package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.InventoryClickEventSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects AutoTotem — a macro or client mod that automatically moves a totem
 * of undying into the offhand slot within milliseconds of the previous totem
 * being consumed (or preemptively at low health).
 *
 * <p>Human players cannot consistently react and click within 100ms to re-equip
 * a totem.  AutoTotem mods swap the totem programmatically, producing offhand
 * swap events with sub-human intervals.
 *
 * <p>Detection relies on the offhand slot (slot 40 in player inventory) being
 * targeted in a very rapid inventory click.  A window of offhand swap intervals
 * is maintained; sustained sub-human intervals are flagged.
 */
public final class AutoTotemCheck extends AbstractWindowCheck {

  /**
   * Maximum mean offhand-swap interval (nanoseconds) considered human-possible.
   * 180ms = fast human reaction; below this = likely automated.
   */
  private static final double MIN_HUMAN_SWAP_NS = 180_000_000D;

  public AutoTotemCheck(int limit) {
    super(limit, 5);
  }

  @Override
  public String name() {
    return "AutoTotem";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof InventoryClickEventSignal click)) {
      return CheckResult.clean(name(), category());
    }

    if (!click.offhandSwap() || click.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    var ws = stats.record(click.playerId(), click.intervalNanos());
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() < MIN_HUMAN_SWAP_NS && ws.isUniformlyCadenced(0.20)) {
      int buf = incrementBuffer(click.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(),
            "AutoTotem offhand swap (meanInterval=" + String.format("%.0f", ws.mean() / 1_000_000D) + "ms)",
            Math.min(1.0D, buf / 4.0D),
            false);
      }
    } else {
      coolDown(click.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
