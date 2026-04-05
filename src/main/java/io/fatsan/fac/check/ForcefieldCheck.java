package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatContextSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects forcefield / multi-directional killaura by checking victim angular spread.
 *
 * <p><b>Cheat behaviour:</b> A forcefield module attacks all nearby players or
 * mobs regardless of the attacker's facing direction.  In a short time window,
 * the attacked targets will be distributed around the attacker in all compass
 * directions — something that is statistically impossible in legitimate PvP where
 * a player can only face one direction at a time.
 *
 * <p><b>Detection method:</b> For each hit, we compute the horizontal angle
 * (bearing) from the attacker to the victim in degrees [0, 360).  We maintain a
 * rolling window of the last {@value #WINDOW} bearings per player.  If the
 * angular spread (max − min, circularly) across the window exceeds
 * {@value #SPREAD_THRESHOLD_DEG}°, the player is flagged.
 *
 * <p><b>False-positive mitigations:</b>
 * <ul>
 *   <li>Window of 6 hits required before evaluating.</li>
 *   <li>Spread threshold of 270° — legitimate quick-360 PvP rarely exceeds
 *       this in a 6-hit window.</li>
 *   <li>Buffer of 4 before flagging.</li>
 * </ul>
 *
 * <p><b>Folia thread safety:</b> per-player deque in {@code ConcurrentHashMap};
 * windows are per-player and never shared.
 */
public final class ForcefieldCheck extends AbstractBufferedCheck {

  private static final int WINDOW = 6;
  private static final double SPREAD_THRESHOLD_DEG = 270.0;

  /** Per-player circular buffer of recent victim bearings (as int degrees 0-359). */
  private final Map<String, int[]> bearingWindows = new ConcurrentHashMap<>();
  /** Per-player write pointer into the circular buffer. */
  private final Map<String, Integer> writePointers = new ConcurrentHashMap<>();

  public ForcefieldCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "Forcefield";
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

    // Compute horizontal bearing from attacker to victim
    double dx = ctx.victimX() - ctx.attackerX();
    double dz = ctx.victimZ() - ctx.attackerZ();
    int bearing = (int) ((Math.toDegrees(Math.atan2(dz, dx)) + 360.0) % 360.0);

    // Record in per-player circular window
    int[] window = bearingWindows.computeIfAbsent(ctx.playerId(), k -> new int[WINDOW]);
    int ptr = writePointers.merge(ctx.playerId(), 1, (a, b) -> (a + 1) % WINDOW);
    window[ptr] = bearing;

    // Need a full window before evaluating
    int count = writePointers.getOrDefault(ctx.playerId(), 0);
    if (count < WINDOW) {
      return CheckResult.clean(name(), category());
    }

    // Compute angular spread across the window (circular, handles wrap-around)
    double spread = circularSpread(window);

    if (spread >= SPREAD_THRESHOLD_DEG) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Forcefield: victim spread=" + String.format("%.0f", spread) + "° buf=" + buf,
            Math.min(1.0, buf / 6.0),
            true);
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }

  /**
   * Computes the circular angular spread of a set of bearings.
   * Returns the arc length (in degrees) of the smallest arc that contains all points.
   */
  private static double circularSpread(int[] bearings) {
    // Sort a copy and compute max gap — the spread = 360 - maxGap
    int[] sorted = bearings.clone();
    java.util.Arrays.sort(sorted);
    double maxGap = 0;
    for (int i = 1; i < sorted.length; i++) {
      maxGap = Math.max(maxGap, sorted[i] - sorted[i - 1]);
    }
    // Wrap-around gap
    maxGap = Math.max(maxGap, sorted[0] + 360 - sorted[sorted.length - 1]);
    return 360.0 - maxGap;
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    bearingWindows.remove(playerId);
    writePointers.remove(playerId);
  }
}
