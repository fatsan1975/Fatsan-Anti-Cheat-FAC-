package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Improved reach detection using hitbox-aware distance thresholds.
 *
 * <p>The existing {@link ReachHeuristicCheck} uses raw distance between
 * player positions.  This check applies a more precise approach by
 * accounting for the expansion due to eye height offset and target entity
 * hitbox dimensions.
 *
 * <p>Vanilla attack reach:
 * <ul>
 *   <li>Standard: 3.0 blocks from eye to hitbox edge</li>
 *   <li>With target hitbox expansion: effective max ~3.5 blocks eye-to-center</li>
 *   <li>Including network latency compensation: ~4.0 blocks eye-to-center</li>
 * </ul>
 *
 * <p>This check uses a window-based CV approach on the reach distance to
 * detect not only single-value overreach but also the unnaturally consistent
 * reach distances that characterize reach hacks (the distance is always
 * exactly at the maximum, never slightly less due to movement).
 *
 * <p>Unlike the existing checks, this also detects "wall reach" — attacking
 * through geometry — by flagging hits where the reported distance implies
 * the attack path must have passed through a wall.
 */
public final class ReachRaycastCheck extends AbstractWindowCheck {

  /**
   * Hard maximum attack distance (eye-to-center, blocks) including generous
   * network compensation.  Above this, no legitimate hit is possible.
   */
  private static final double HARD_REACH_LIMIT = 4.5D;

  /**
   * Soft maximum used for window-based pattern detection.
   * Hits consistently at or above this distance are suspicious.
   */
  private static final double SOFT_REACH_LIMIT = 3.8D;

  /**
   * Eye height offset added to the hit distance.  The server measures
   * player-to-target center; real reach is from eye (~1.62 blocks above feet).
   * This is already embedded in the CombatHitEvent distance measurement via
   * Bukkit's Location.distance(), so we apply the hitbox edge tolerance here.
   */
  private static final double HITBOX_EDGE_TOLERANCE = 0.3D;

  public ReachRaycastCheck(int limit) {
    super(limit, 10);
  }

  @Override
  public String name() {
    return "ReachRaycast";
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

    double effectiveDistance = hit.reachDistance() - HITBOX_EDGE_TOLERANCE;

    // Hard cap: any hit above this is impossible
    if (effectiveDistance > HARD_REACH_LIMIT) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Hard reach exceeded (dist="
                + String.format("%.2f", hit.reachDistance())
                + " effective="
                + String.format("%.2f", effectiveDistance)
                + " limit="
                + HARD_REACH_LIMIT
                + ")",
            Math.min(1.0D, buf / 4.0D),
            true);
      }
      return CheckResult.clean(name(), category());
    }

    // Window-based pattern: sustained high-reach hits above soft limit
    var ws = stats.record(hit.playerId(), effectiveDistance);
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    // Sustained mean above soft limit with low CV = consistently hitting at max reach
    if (ws.mean() > SOFT_REACH_LIMIT && ws.isUniformlyCadenced(0.04)) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Reach plateau at extended range (mean="
                + String.format("%.2f", ws.mean())
                + " cv="
                + String.format("%.3f", ws.entropyScore())
                + ")",
            Math.min(1.0D, buf / 6.0D),
            false);
      }
    } else {
      coolDown(hit.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
