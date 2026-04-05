package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects low-entropy combat click intervals — a pattern consistent with
 * auto-clicker or KillAura clients that produce suspiciously uniform hit timing.
 *
 * <p>Legitimate players show natural variance in their attack intervals due to
 * latency jitter, target movement, and human motor control.  When the
 * coefficient of variation of intervals collapses near zero within a short
 * window, the behaviour resembles a bot or macro.
 *
 * <p>Uses the shared {@link AbstractWindowCheck} base class to avoid duplicating
 * sliding-window and entropy-computation logic.
 */
public final class CombatIntervalEntropyCheck extends AbstractWindowCheck {

  /**
   * Maximum interval (ms) considered suspicious.  Very fast, uniform clicks
   * are the primary target; legitimate fast PvP rarely sustains &lt;100 ms
   * intervals over many consecutive hits.
   */
  private static final double MAX_SUSPICIOUS_INTERVAL_MS = 100.0;

  /**
   * Maximum coefficient of variation for the interval window to be considered
   * "low entropy".  Below this value the timing is unnaturally consistent.
   */
  private static final double MAX_CV_LOW_ENTROPY = 0.05D;


  public CombatIntervalEntropyCheck(int limit) {
    super(limit, 8);
  }

  @Override
  public String name() {
    return "CombatIntervalEntropy";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit) || hit.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    double intervalMs = hit.intervalNanos() / 1_000_000.0;
    var ws = stats.record(hit.playerId(), intervalMs);

    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() < MAX_SUSPICIOUS_INTERVAL_MS && ws.isUniformlyCadenced(MAX_CV_LOW_ENTROPY)) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Low-entropy combat interval window (mean="
                + String.format("%.1f", ws.mean())
                + "ms cv="
                + String.format("%.4f", ws.entropyScore())
                + ")",
            Math.min(1.0D, buf / 8.0D),
            false);
      }
    } else {
      coolDown(hit.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}

