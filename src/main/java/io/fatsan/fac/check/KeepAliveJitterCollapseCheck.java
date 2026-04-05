package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects unnaturally uniform (jitter-collapsed) ping values — consistent with
 * ping-spoof or fake-lag clients that synthesise a constant latency rather than
 * exhibiting the natural variance present in real network conditions.
 *
 * <p>Real network latency fluctuates due to routing, buffering, and OS
 * scheduling noise.  A coefficient of variation near zero over a sustained
 * window of keepalive samples, combined with a non-trivial baseline ping,
 * is characteristic of spoofed or manipulated ping values.
 *
 * <p>Uses {@link AbstractWindowCheck} instead of two separate maps
 * ({@code lastPing} + {@code stableTicks}) that could be bypassed by
 * inserting a single differing sample every few ticks.
 */
public final class KeepAliveJitterCollapseCheck extends AbstractWindowCheck {

  /**
   * Minimum ping baseline (ms) above which a jitter collapse is suspicious.
   * Very low pings naturally have low absolute jitter; we focus on mid-range
   * pings that should show measurable variance.
   */
  private static final double MIN_SUSPICIOUS_PING_MS = 30.0;

  /** Maximum CV for the ping window to be considered "jitter-collapsed". */
  private static final double MAX_CV_JITTER_COLLAPSED = 0.03D;


  public KeepAliveJitterCollapseCheck(int limit) {
    super(limit, 10);
  }

  @Override
  public String name() {
    return "KeepAliveJitterCollapse";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof KeepAliveSignal keepAlive)) {
      return CheckResult.clean(name(), category());
    }

    var ws = stats.record(keepAlive.playerId(), (double) keepAlive.pingMillis());

    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() >= MIN_SUSPICIOUS_PING_MS
        && ws.isUniformlyCadenced(MAX_CV_JITTER_COLLAPSED)) {
      int buf = incrementBuffer(keepAlive.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Ping jitter collapsed over sustained window (mean="
                + String.format("%.0f", ws.mean())
                + "ms cv="
                + String.format("%.4f", ws.entropyScore())
                + ")",
            Math.min(1.0D, buf / 6.0D),
            false);
      }
    } else {
      coolDown(keepAlive.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}

