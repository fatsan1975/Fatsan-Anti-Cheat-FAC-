package io.fatsan.fac.check.support;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared feature extractor for checks that operate on sliding-window statistics.
 *
 * <p>Tracks a bounded sliding window of {@code double} values per player and
 * exposes pre-computed statistics (mean, variance, oscillation count, entropy
 * score) so that the variance / entropy / oscillation / plateau / collapse check
 * families do not each maintain their own duplicate window logic.
 *
 * <p>Thread-safe via per-player {@code synchronized} access on the window deque.
 * Designed for single-instance use per check (each check that needs a window
 * creates its own {@code WindowStatsTracker}).
 */
public final class WindowStatsTracker {

  /** Minimum window fill before {@link Stats#hasEnoughData()} returns true. */
  private static final int MIN_DATA_WINDOW = 3;

  private final int maxWindowSize;
  private final Map<String, Deque<Double>> windows = new ConcurrentHashMap<>();

  /**
   * @param maxWindowSize maximum number of values retained per player.  Values
   *                      outside this range are silently clamped to [4, 256].
   */
  public WindowStatsTracker(int maxWindowSize) {
    this.maxWindowSize = Math.max(4, Math.min(256, maxWindowSize));
  }

  /**
   * Records a new value for the player and returns computed statistics over
   * the updated window.
   *
   * @param playerId the player's UUID string
   * @param value    the new observation to add
   * @return {@link Stats} computed from the current window, or
   *         {@link Stats#EMPTY} if there are fewer than
   *         {@value MIN_DATA_WINDOW} observations
   */
  public Stats record(String playerId, double value) {
    Deque<Double> window = windows.computeIfAbsent(playerId, k -> new ArrayDeque<>());
    synchronized (window) {
      window.addLast(value);
      while (window.size() > maxWindowSize) {
        window.removeFirst();
      }
      if (window.size() < MIN_DATA_WINDOW) {
        return Stats.EMPTY;
      }
      return compute(window);
    }
  }

  /**
   * Returns the current stats without adding a new value.  Returns
   * {@link Stats#EMPTY} if the player has no window or insufficient data.
   */
  public Stats peek(String playerId) {
    Deque<Double> window = windows.get(playerId);
    if (window == null) {
      return Stats.EMPTY;
    }
    synchronized (window) {
      if (window.size() < MIN_DATA_WINDOW) {
        return Stats.EMPTY;
      }
      return compute(window);
    }
  }

  /** Removes all state for the player. */
  public void clear(String playerId) {
    windows.remove(playerId);
  }

  // ── Internal computation ──────────────────────────────────────────────────

  private static Stats compute(Deque<Double> window) {
    int n = window.size();
    double[] values = new double[n];
    int idx = 0;
    for (double v : window) {
      values[idx++] = v;
    }
    double mean = mean(values);
    double variance = variance(values, mean);
    int oscillations = oscillations(values);
    double entropyScore = entropyScore(variance, mean);
    boolean flat = variance < 1_000_000L; // callers provide nanos; caller decides threshold
    return new Stats(n, mean, variance, oscillations, entropyScore);
  }

  private static double mean(double[] values) {
    double sum = 0.0;
    for (double v : values) {
      sum += v;
    }
    return sum / values.length;
  }

  private static double variance(double[] values, double mean) {
    double sum = 0.0;
    for (double v : values) {
      double diff = v - mean;
      sum += diff * diff;
    }
    return sum / values.length;
  }

  /**
   * Counts local extrema (peaks and valleys) in the value sequence.
   * A value at position {@code i} is a local extremum if it is strictly
   * greater than both neighbours (peak) or strictly less (valley).
   */
  private static int oscillations(double[] values) {
    int count = 0;
    for (int i = 1; i < values.length - 1; i++) {
      boolean peak = values[i] > values[i - 1] && values[i] > values[i + 1];
      boolean valley = values[i] < values[i - 1] && values[i] < values[i + 1];
      if (peak || valley) {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns a normalised coefficient-of-variation in [0, 1].
   * Near 0 = values are very uniform (potential bot-like cadence lock).
   * Near 1 = values are very spread out (high natural jitter or burst anomaly).
   */
  private static double entropyScore(double variance, double mean) {
    if (variance <= 0.0) {
      return 0.0;
    }
    double stddev = Math.sqrt(variance);
    double cv = stddev / Math.max(Math.abs(mean), 1.0);
    return Math.min(1.0, cv);
  }

  // ── Stats record ─────────────────────────────────────────────────────────

  /**
   * Immutable snapshot of window statistics for a single player at a point in
   * time.
   */
  public record Stats(
      int windowSize,
      double mean,
      double variance,
      int oscillations,
      double entropyScore) {

    /** Sentinel returned when not enough data is available yet. */
    public static final Stats EMPTY = new Stats(0, 0.0, 0.0, 0, 0.0);

    /** True when the window contains at least {@value MIN_DATA_WINDOW} values. */
    public boolean hasEnoughData() {
      return windowSize >= MIN_DATA_WINDOW;
    }

    /** Standard deviation derived from {@link #variance()}. */
    public double stddev() {
      return Math.sqrt(variance);
    }

    /**
     * True when the variance is at or below {@code maxVariance}.
     * Useful for cadence-lock / plateau detection.
     */
    public boolean isFlat(double maxVariance) {
      return variance <= maxVariance;
    }

    /**
     * True when at least {@code minOscillations} local extrema are present.
     * Useful for oscillation / sawtooth detection.
     */
    public boolean isOscillating(int minOscillations) {
      return oscillations >= minOscillations;
    }

    /**
     * True when the coefficient of variation is at or below {@code maxCv}.
     * Low CV = unnaturally uniform timing, consistent with macro / bot patterns.
     */
    public boolean isUniformlyCadenced(double maxCv) {
      return hasEnoughData() && entropyScore <= maxCv;
    }
  }
}
