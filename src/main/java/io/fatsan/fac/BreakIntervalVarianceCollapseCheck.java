package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockBreakEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects unnaturally uniform block-break intervals — consistent with
 * speed-mine or nuker clients that maintain a mechanically precise
 * break cadence with near-zero natural variance.
 *
 * <p>Legitimate players show natural jitter in block-break timing even
 * when using macros for repetitive mining.  When the coefficient of
 * variation collapses near zero and the mean interval is suspiciously
 * fast, the pattern is flagged.
 *
 * <p>Uses {@link AbstractWindowCheck} instead of the previous single-value
 * {@code last} map approach, which could only compare one consecutive pair
 * and was therefore easily bypassed by inserting occasional outlier values.
 */
public final class BreakIntervalVarianceCollapseCheck extends AbstractWindowCheck {

  /** Maximum mean interval (ms) considered fast enough to be suspicious. */
  private static final double MAX_SUSPICIOUS_MEAN_MS = 120.0;

  /** Maximum CV for the interval window to be considered "collapsed". */
  private static final double MAX_CV_COLLAPSED = 0.03D;


  public BreakIntervalVarianceCollapseCheck(int limit) {
    super(limit, 8);
  }

  @Override
  public String name() {
    return "BreakIntervalVarianceCollapse";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockBreakEventSignal blockBreak)
        || blockBreak.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    double intervalMs = blockBreak.intervalNanos() / 1_000_000.0;
    var ws = stats.record(blockBreak.playerId(), intervalMs);

    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() < MAX_SUSPICIOUS_MEAN_MS && ws.isUniformlyCadenced(MAX_CV_COLLAPSED)) {
      int buf = incrementBuffer(blockBreak.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Block-break interval variance collapsed (mean="
                + String.format("%.1f", ws.mean())
                + "ms cv="
                + String.format("%.4f", ws.entropyScore())
                + ")",
            Math.min(1.0D, buf / 8.0D),
            false);
      }
    } else {
      coolDown(blockBreak.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}

