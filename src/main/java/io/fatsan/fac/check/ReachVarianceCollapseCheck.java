package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects unnaturally uniform reach distances — a pattern consistent with
 * reach-hack clients that maintain a fixed attack distance with near-zero
 * natural variance.
 *
 * <p>Legitimate players exhibit small fluctuations in reach distance due to
 * latency, movement interpolation, and target-entity hitbox changes.  A
 * suspicious collapse of variance (very low CV) combined with a mean distance
 * above the vanilla interaction threshold is flagged.
 *
 * <p>Uses the shared {@link AbstractWindowCheck} base class to avoid duplicating
 * sliding-window and variance-computation logic.
 */
public final class ReachVarianceCollapseCheck extends AbstractWindowCheck {

  /** Mean reach distance above which variance collapse becomes suspicious. */
  private static final double SUSPICIOUS_MEAN_THRESHOLD = 3.1D;

  /**
   * Maximum coefficient-of-variation (stddev / mean) considered "collapsed".
   * Below this, the timing is unnaturally uniform.
   */
  private static final double MAX_CV_COLLAPSED = 0.025D;


  public ReachVarianceCollapseCheck(int limit) {
    super(limit, 8);
  }

  @Override
  public String name() {
    return "ReachVarianceCollapse";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) {
      return CheckResult.clean(name(), category());
    }

    var ws = stats.record(hit.playerId(), hit.reachDistance());
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() > SUSPICIOUS_MEAN_THRESHOLD && ws.isUniformlyCadenced(MAX_CV_COLLAPSED)) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Reach variance collapsed around elevated mean (mean="
                + String.format("%.2f", ws.mean())
                + " cv="
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

