package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.FishingSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects auto-fishing bots by identifying perfectly timed fish catches.
 *
 * <p><b>Cheat behaviour:</b> Auto-fishing macros listen for the bobber
 * splash sound or particle and instantly retract/recast the rod at the
 * optimal millisecond.  This produces fish catch intervals that are:
 * <ul>
 *   <li>Uniformly short (close to the minimum fish wait time of ~5s).</li>
 *   <li>Near-zero variance across many catches (bots react at the exact tick).</li>
 * </ul>
 * Human fishing has high reaction-time variance (100–600ms after the splash).
 *
 * <p><b>Detection:</b> Track the standard deviation of {@code intervalNanos}
 * across the last {@value #WINDOW_SIZE} fish catches.  Flag when stdDev is
 * below {@value #VARIANCE_THRESHOLD_MS}ms (nearly robotic precision) and
 * mean catch time is within the expected fishing window.
 *
 * <p>Uses {@link AbstractWindowCheck} to compute variance.
 */
public final class AutoFishCheck extends AbstractWindowCheck {

  private static final int WINDOW_SIZE = 8;
  /** Below this stdDev in ms, the timing is inhuman. */
  private static final double VARIANCE_THRESHOLD_MS = 100.0;
  /** Minimum plausible fish interval (vanilla min wait ~5s). */
  private static final long MIN_INTERVAL_NS = 4_000_000_000L;
  /** Maximum: if someone fishes slower than 90s, reset tracking. */
  private static final long MAX_INTERVAL_NS = 90_000_000_000L;

  public AutoFishCheck(int limit) {
    super(limit, WINDOW_SIZE);
  }

  @Override
  public String name() {
    return "AutoFish";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof FishingSignal signal)) {
      return CheckResult.clean(name(), category());
    }

    if (signal.intervalNanos() == Long.MAX_VALUE
        || signal.intervalNanos() < MIN_INTERVAL_NS
        || signal.intervalNanos() > MAX_INTERVAL_NS) {
      stats.clear(signal.playerId());
      return CheckResult.clean(name(), category());
    }

    // Record in milliseconds for human-readable output
    long intervalMs = signal.intervalNanos() / 1_000_000;
    var s = stats.record(signal.playerId(), intervalMs);

    if (!s.hasEnoughData()) return CheckResult.clean(name(), category());

    if (s.stddev() < VARIANCE_THRESHOLD_MS) {
      int buf = incrementBuffer(signal.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "AutoFish: catch stdDev=" + String.format("%.1f", s.stddev())
                + "ms mean=" + String.format("%.0f", s.mean()) + "ms buf=" + buf,
            Math.min(0.85, buf / 5.0),
            false);
      }
    } else {
      coolDown(signal.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
