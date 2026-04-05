package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockBreakContextSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects automated mining — breaking the same type of block at perfectly
 * uniform intervals, characteristic of a bot running a mining script.
 *
 * <p><b>Cheat behaviour:</b> Mining bots (e.g. Baritone with auto-mine) dig
 * tunnels at precise, uniform intervals.  Humans vary their break timing due to
 * reaction delay, mouse drift, and ping variance.  A bot produces break intervals
 * with variance near zero for the same block type.
 *
 * <p><b>Detection:</b> We use {@link AbstractWindowCheck} to track the standard
 * deviation of break intervals for each player.  We only evaluate when
 * consecutive breaks are of the same material type (the bot mines a uniform
 * vein or tunnel).
 *
 * <p><b>Alert-only:</b> Survival players using AutoHotkey or similar might have
 * low variance on easy blocks.  This is a supporting signal, not primary evidence.
 */
public final class AutoMineCheck extends AbstractWindowCheck {

  private static final int WINDOW_SIZE = 12;
  private static final double VARIANCE_THRESHOLD_MS = 30.0;
  private static final long MIN_INTERVAL_NS = 100_000_000L; // 100ms
  private static final long MAX_INTERVAL_NS = 2_000_000_000L; // 2s

  private final java.util.Map<String, String> lastMaterial = new java.util.concurrent.ConcurrentHashMap<>();

  public AutoMineCheck(int limit) {
    super(limit, WINDOW_SIZE);
  }

  @Override
  public String name() {
    return "AutoMine";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(io.fatsan.fac.model.NormalizedEvent event) {
    if (!(event instanceof BlockBreakContextSignal ctx)) {
      return CheckResult.clean(name(), category());
    }

    if (ctx.intervalNanos() == Long.MAX_VALUE
        || ctx.intervalNanos() < MIN_INTERVAL_NS
        || ctx.intervalNanos() > MAX_INTERVAL_NS) {
      // First break, too fast (nuker handles that), or big gap — reset
      stats.clear(ctx.playerId());
      lastMaterial.put(ctx.playerId(), ctx.blockMaterialName());
      return CheckResult.clean(name(), category());
    }

    // Only track when breaking the same material repeatedly
    String prev = lastMaterial.get(ctx.playerId());
    lastMaterial.put(ctx.playerId(), ctx.blockMaterialName());

    if (!ctx.blockMaterialName().equals(prev)) {
      stats.clear(ctx.playerId());
      return CheckResult.clean(name(), category());
    }

    long intervalMs = ctx.intervalNanos() / 1_000_000;
    var s = stats.record(ctx.playerId(), intervalMs);
    if (!s.hasEnoughData()) return CheckResult.clean(name(), category());

    if (s.stddev() < VARIANCE_THRESHOLD_MS && s.mean() > 150) {
      // Mean > 150ms: the break is not instant (nuker), yet perfectly timed
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "AutoMine: " + ctx.blockMaterialName()
                + " stdDev=" + String.format("%.1f", s.stddev())
                + "ms mean=" + String.format("%.0f", s.mean()) + "ms buf=" + buf,
            Math.min(0.75, buf / 6.0),
            false);
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    lastMaterial.remove(playerId);
  }
}
