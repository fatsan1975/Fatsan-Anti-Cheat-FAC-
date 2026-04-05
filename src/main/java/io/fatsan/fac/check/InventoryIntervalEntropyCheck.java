package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.InventoryClickEventSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects low-entropy inventory click intervals — a pattern consistent with
 * inventory-macro or auto-sort clients that produce suspiciously uniform
 * click timing.
 *
 * <p>Legitimate players exhibit natural variance in their inventory interactions
 * due to reaction time and hand–eye coordination.  When the coefficient of
 * variation of click intervals collapses near zero within a short window,
 * the behaviour is consistent with a macro or bot.
 *
 * <p>Uses {@link AbstractWindowCheck} to avoid duplicate sliding-window and
 * entropy-computation logic shared with the combat and block-break families.
 */
public final class InventoryIntervalEntropyCheck extends AbstractWindowCheck {

  /** Maximum mean interval (ms) to be considered fast enough to be suspicious. */
  private static final double MAX_SUSPICIOUS_MEAN_MS = 120.0;

  /** Maximum CV for the interval window to be considered "low entropy". */
  private static final double MAX_CV_LOW_ENTROPY = 0.04D;


  public InventoryIntervalEntropyCheck(int limit) {
    super(limit, 8);
  }

  @Override
  public String name() {
    return "InventoryIntervalEntropy";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.INVENTORY;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof InventoryClickEventSignal inv) || inv.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    double intervalMs = inv.intervalNanos() / 1_000_000.0;
    var ws = stats.record(inv.playerId(), intervalMs);

    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() < MAX_SUSPICIOUS_MEAN_MS && ws.isUniformlyCadenced(MAX_CV_LOW_ENTROPY)) {
      int buf = incrementBuffer(inv.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Low-entropy inventory click interval window (mean="
                + String.format("%.1f", ws.mean())
                + "ms cv="
                + String.format("%.4f", ws.entropyScore())
                + ")",
            Math.min(1.0D, buf / 8.0D),
            false);
      }
    } else {
      coolDown(inv.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}

