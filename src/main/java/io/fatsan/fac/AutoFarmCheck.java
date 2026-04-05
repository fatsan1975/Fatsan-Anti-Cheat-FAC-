package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockBreakContextSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayDeque;

/**
 * Detects automated crop farming — breaking crops at inhuman speed and regularity.
 *
 * <p><b>Cheat behaviour:</b> Auto-farm macros or bots break crops (wheat, carrots,
 * potatoes, beetroot, nether wart, etc.) at precisely timed intervals with
 * near-zero variance.  Humans show irregular timing; bots produce variance ≈ 0.
 *
 * <p><b>Detection:</b>
 * <ol>
 *   <li>Collect the last {@value #WINDOW_SIZE} break-interval values for crop materials.</li>
 *   <li>If the standard deviation of intervals is below {@value #VARIANCE_THRESHOLD_MS}ms
 *       and all intervals are below {@value #MAX_INTERVAL_MS}ms (rapid), flag.</li>
 * </ol>
 *
 * <p><b>Alert-only:</b> Players using auto-clicker macros for farming are common
 * on survival servers.  This check produces an alert for admin review.
 */
public final class AutoFarmCheck extends AbstractWindowCheck {

  private static final int WINDOW_SIZE = 10;
  private static final double VARIANCE_THRESHOLD_MS = 25.0;
  private static final long MAX_INTERVAL_MS = 500;

  private final Map<String, ArrayDeque<Long>> cropIntervals = new ConcurrentHashMap<>();

  public AutoFarmCheck(int limit) {
    super(limit, WINDOW_SIZE);
  }

  @Override
  public String name() {
    return "AutoFarm";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockBreakContextSignal ctx)) {
      return CheckResult.clean(name(), category());
    }

    if (!isCrop(ctx.blockMaterialName())) {
      return CheckResult.clean(name(), category());
    }

    if (ctx.intervalNanos() == Long.MAX_VALUE || ctx.intervalNanos() <= 0) {
      return CheckResult.clean(name(), category());
    }

    long intervalMs = ctx.intervalNanos() / 1_000_000;
    if (intervalMs > MAX_INTERVAL_MS) {
      // Gap too large — reset tracking
      cropIntervals.remove(ctx.playerId());
      stats.clear(ctx.playerId());
      coolDown(ctx.playerId());
      return CheckResult.clean(name(), category());
    }

    // Record interval in the window stats (in ms for human-readable variance)
    var s = stats.record(ctx.playerId(), intervalMs);
    if (!s.hasEnoughData()) return CheckResult.clean(name(), category());

    if (s.stddev() < VARIANCE_THRESHOLD_MS) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "AutoFarm: crop break stdDev=" + String.format("%.1f", s.stddev())
                + "ms mean=" + String.format("%.0f", s.mean()) + "ms buf=" + buf,
            Math.min(0.8, buf / 5.0),
            false); // alert-only
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }

  private static boolean isCrop(String materialName) {
    if (materialName == null) return false;
    String m = materialName.toUpperCase();
    return m.equals("WHEAT") || m.equals("CARROTS") || m.equals("POTATOES")
        || m.equals("BEETROOTS") || m.equals("NETHER_WART")
        || m.equals("SWEET_BERRY_BUSH") || m.equals("COCOA")
        || m.equals("SUGAR_CANE") || m.equals("MELON") || m.equals("PUMPKIN");
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    cropIntervals.remove(playerId);
  }
}
