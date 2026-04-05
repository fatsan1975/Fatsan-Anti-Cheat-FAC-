package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects sustained plateau patterns in combat hit intervals — a pattern
 * consistent with auto-clicker or KillAura clients maintaining a mechanically
 * precise attack cadence within a narrow speed band.
 *
 * <p>The previous single-pair comparison ({@code |prev - cur| <= 1ms}) was
 * bypassable by inserting a single outlier value every few hits.  The
 * WindowStatsTracker-based approach requires the CV to collapse over a
 * full window, making it harder to bypass with occasional timing noise.
 */
public final class CombatHitIntervalPlateauCheck extends AbstractWindowCheck {

  /** Minimum mean interval (ms) — very short intervals are handled by other checks. */
  private static final double MIN_PLATEAU_MEAN_MS = 60.0;

  /** Maximum mean interval (ms) — above this, timing is slow enough to be legitimate. */
  private static final double MAX_PLATEAU_MEAN_MS = 110.0;

  /** Maximum CV for the interval window to qualify as a plateau. */
  private static final double MAX_CV_PLATEAU = 0.025D;


  public CombatHitIntervalPlateauCheck(int limit) {
    super(limit, 8);
  }

  @Override
  public String name() {
    return "CombatHitIntervalPlateau";
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

    if (ws.mean() >= MIN_PLATEAU_MEAN_MS
        && ws.mean() <= MAX_PLATEAU_MEAN_MS
        && ws.isUniformlyCadenced(MAX_CV_PLATEAU)) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Hit interval plateau sustained over window (mean="
                + String.format("%.1f", ws.mean())
                + "ms cv="
                + String.format("%.4f", ws.entropyScore())
                + ")",
            Math.min(1.0D, buf / 7.0D),
            false);
      }
    } else {
      coolDown(hit.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}

