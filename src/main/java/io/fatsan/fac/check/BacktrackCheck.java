package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatContextSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects backtrack / time-warp cheats that exploit stale entity positions.
 *
 * <p><b>Cheat behaviour:</b> "Backtrack" cheats delay the client's outgoing
 * movement packets so that the server believes the player is still at an older
 * position while the client visually renders a newer one.  The player then
 * attacks the real (newer) entity position that the server reports, but from
 * a position that the server recorded earlier — producing a larger-than-expected
 * effective reach distance.
 *
 * <p><b>Detection method:</b> We flag when {@link CombatContextSignal#reachDistance()}
 * is consistently elevated above the standard 3.0-block reach threshold across
 * multiple consecutive hits.  A hit at 3.0–3.35 is within vanilla tolerance;
 * repeated hits at 3.35–5.0 with no intervening clean hits suggest a desync.
 *
 * <p>This complements {@code ReachHeuristicCheck} (which flags on any single
 * elevated reach) with a pattern-based approach that catches slower, more
 * careful cheats.
 *
 * <p><b>Thresholds:</b>
 * <ul>
 *   <li>3.35 – 4.5 blocks: suspicious range (vanilla max ~3.3, box allowance 3.5)</li>
 *   <li>Buffer of 5: sustained pattern needed</li>
 * </ul>
 *
 * <p><b>Folia thread safety:</b> immutable record, buffer in ConcurrentHashMap.
 */
public final class BacktrackCheck extends AbstractBufferedCheck {

  /** Lower bound of the "suspicious but not impossible" reach range. */
  private static final double REACH_LOWER = 3.35;
  /** Upper bound — above this ReachHeuristicCheck already fires. */
  private static final double REACH_UPPER = 4.8;

  public BacktrackCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "Backtrack";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatContextSignal ctx)) {
      return CheckResult.clean(name(), category());
    }

    double reach = ctx.reachDistance();
    if (reach >= REACH_LOWER && reach <= REACH_UPPER) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Sustained elevated reach (reach=" + String.format("%.2f", reach)
                + " buf=" + buf + ") — possible backtrack",
            Math.min(1.0, buf / 7.0),
            false); // alert-only: backtrack detection has fp risk on high-latency
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
