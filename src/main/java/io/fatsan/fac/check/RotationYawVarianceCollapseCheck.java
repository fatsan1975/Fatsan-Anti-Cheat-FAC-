package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.RotationEvent;

/**
 * Detects unnaturally uniform yaw delta magnitudes — consistent with aim-assist
 * or KillAura clients that maintain a mechanically stable rotation speed while
 * tracking a target.
 *
 * <p>Legitimate players produce natural variance in their yaw changes due to
 * mouse sensitivity, hand movement, and target velocity.  When the magnitude
 * of consecutive yaw deltas collapses to a near-constant value above a
 * meaningful threshold, the pattern is consistent with assisted aim.
 *
 * <p>Uses {@link AbstractWindowCheck} instead of the previous single-pair
 * streak counter approach.
 */
public final class RotationYawVarianceCollapseCheck extends AbstractWindowCheck {

  /** Minimum mean absolute yaw delta (degrees/event) to be suspicious. */
  private static final double MIN_MEAN_YAW_DELTA = 5.0;

  /** Maximum CV for the yaw-delta window to be considered "collapsed". */
  private static final double MAX_CV_COLLAPSED = 0.02D;


  public RotationYawVarianceCollapseCheck(int limit) {
    super(limit, 8);
  }

  @Override public String name() { return "RotationYawVarianceCollapse"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent r)) {
      return CheckResult.clean(name(), category());
    }

    var ws = stats.record(r.playerId(), (double) Math.abs(r.deltaYaw()));

    if (!ws.hasEnoughData()) return CheckResult.clean(name(), category());

    if (ws.mean() >= MIN_MEAN_YAW_DELTA && ws.isUniformlyCadenced(MAX_CV_COLLAPSED)) {
      int buf = incrementBuffer(r.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(),
            "Yaw delta variance collapsed (mean="
                + String.format("%.2f", ws.mean())
                + "deg cv=" + String.format("%.4f", ws.entropyScore()) + ")",
            Math.min(1.0D, buf / 7.0D), false);
      }
    } else {
      coolDown(r.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
