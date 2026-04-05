package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects unnaturally constant hit distances — consistent with reach-hack
 * clients that maintain a mechanically fixed attack range regardless of
 * movement or target position.
 *
 * <p>Legitimate players exhibit small fluctuations in reach distance due to
 * latency, movement interpolation, and entity hitbox changes.  A sustained
 * plateau (very low CV over a full window) above the vanilla interaction
 * threshold is a strong signal of a fixed-distance reach modifier.
 *
 * <p>Uses {@link AbstractWindowCheck} instead of the previous streak+last
 * approach, which could be bypassed by inserting a single outlier value
 * every few hits.
 */
public final class CombatHitDistancePlateauCheck extends AbstractWindowCheck {

  /** Mean reach distance above which a variance plateau is suspicious. */
  private static final double MIN_SUSPICIOUS_MEAN = 2.9D;

  /** Maximum CV for the distance window to qualify as a plateau. */
  private static final double MAX_CV_PLATEAU = 0.015D;

  public CombatHitDistancePlateauCheck(int limit) {
    super(limit, 8);
  }

  @Override
  public String name() {
    return "CombatHitDistancePlateau";
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

    if (ws.mean() > MIN_SUSPICIOUS_MEAN && ws.isUniformlyCadenced(MAX_CV_PLATEAU)) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Hit distance plateau over window (mean="
                + String.format("%.2f", ws.mean())
                + " cv="
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
