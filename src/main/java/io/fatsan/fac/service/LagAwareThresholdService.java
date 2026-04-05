package io.fatsan.fac.service;

/**
 * Scales detection thresholds based on current server TPS (ticks per second).
 *
 * <h2>Why TPS-aware thresholds are critical on Folia servers</h2>
 *
 * <p>On large Folia servers, individual regions can lag independently.  When a
 * region runs below 20 TPS, movement events arrive less frequently, causing
 * larger-than-normal position deltas per event.  Without TPS awareness, nearly
 * every movement check produces false positives during server lag:
 *
 * <ul>
 *   <li>{@code SpeedEnvelopeCheck}: reports speed 2× the limit at 10 TPS because
 *       two ticks of movement are bundled into one event.</li>
 *   <li>{@code BunnyHopCheck}: ground-contact intervals double at 10 TPS, making
 *       legitimate players appear to instantly re-jump.</li>
 *   <li>{@code DashHackCheck}: blocks-per-second doubles because the interval
 *       denominator is halved during lag.</li>
 * </ul>
 *
 * <h2>How it works</h2>
 *
 * <p>The Folia {@code GlobalRegionScheduler} calls {@link #updateTps(double)}
 * with the current average TPS on every scheduler heartbeat.  Checks that
 * have velocity or speed thresholds call {@link #scaleThreshold(double)} to
 * multiply their threshold by the inverse TPS ratio:
 *
 * <pre>
 *   effective = threshold * (20.0 / currentTps)
 * </pre>
 *
 * <p>At 20 TPS → multiplier = 1.0 (no change).
 * At 10 TPS → multiplier = 2.0 (threshold doubles, accepting faster movement).
 * At 5 TPS  → multiplier = 4.0 (extreme lag — very permissive).
 *
 * <h2>Cap</h2>
 *
 * <p>The multiplier is capped at {@value #MAX_MULTIPLIER} to avoid completely
 * disabling detection during extreme lag.  At cap, all detection is still active
 * but only flags the most egregious violations.
 *
 * <h2>Thread safety</h2>
 *
 * <p>{@code currentTps} is {@code volatile} — written from the global scheduler
 * thread, read from any region thread without synchronization.  The read may be
 * slightly stale (by one scheduler tick, ~50ms) but this is acceptable.
 */
public final class LagAwareThresholdService {

  private static final double NOMINAL_TPS = 20.0;
  private static final double MAX_MULTIPLIER = 5.0;
  private static final double MIN_TPS = 1.0; // avoid division by zero

  /** Current server TPS, updated by the global scheduler heartbeat. */
  private volatile double currentTps = NOMINAL_TPS;

  /**
   * Called by the global scheduler on every tick heartbeat.
   *
   * @param tps  current average TPS (1-minute average from the global region)
   */
  public void updateTps(double tps) {
    this.currentTps = Math.max(MIN_TPS, Math.min(NOMINAL_TPS, tps));
  }

  /**
   * Returns the current TPS.  Used by checks that need to conditionally skip
   * at extreme lag levels.
   */
  public double currentTps() {
    return currentTps;
  }

  /**
   * Returns {@code true} if the server is lagging enough that detection should
   * be skipped entirely (below {@value #SKIP_BELOW_TPS} TPS).
   *
   * <p>At extreme lag, detection produces only false positives.  This gate
   * protects players from being flagged for server-caused movement anomalies.
   */
  public boolean isLaggingTooMuch() {
    return currentTps < SKIP_BELOW_TPS;
  }

  private static final double SKIP_BELOW_TPS = 8.0;

  /**
   * Scales a vanilla threshold value by the lag multiplier.
   *
   * <p>Example: {@code scaleThreshold(5.6)} at 10 TPS returns {@code 11.2},
   * allowing movement checks to accept 2× normal speed during lag.
   *
   * @param threshold  the baseline vanilla threshold
   * @return  the lag-adjusted threshold, capped at {@value #MAX_MULTIPLIER}×
   */
  public double scaleThreshold(double threshold) {
    double multiplier = NOMINAL_TPS / currentTps;
    multiplier = Math.min(MAX_MULTIPLIER, multiplier);
    return threshold * multiplier;
  }

  /**
   * Scales a time interval threshold (in nanoseconds) by the lag multiplier.
   *
   * <p>Used for checks where intervals become longer during lag (e.g., minimum
   * block break time, hotbar swap interval).
   *
   * @param intervalNanos  baseline minimum interval in nanoseconds
   * @return  the lag-adjusted minimum interval
   */
  public long scaleIntervalNanos(long intervalNanos) {
    double multiplier = NOMINAL_TPS / currentTps;
    multiplier = Math.min(MAX_MULTIPLIER, multiplier);
    return (long) (intervalNanos * multiplier);
  }
}
