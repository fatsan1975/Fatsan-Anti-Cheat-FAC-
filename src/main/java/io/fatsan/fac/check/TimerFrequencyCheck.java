package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects Timer — the cheat that speeds up the client's game timer, causing
 * the client to send packets at a higher-than-normal rate.
 *
 * <p>At 20 ticks per second (normal game speed), a client should send
 * approximately 20 movement packets per second.  A timer hack running at
 * 1.5× speed will send ~30 packets/second.  This check measures the packet
 * rate over a sliding time window and flags sustained elevation above
 * the expected rate.
 *
 * <p>This implementation improves on {@link TimerCadenceCheck} by measuring
 * actual packets-per-second using a real time window rather than relying on
 * interval consistency alone.  Short bursts from lag spikes and reconnects
 * are filtered by requiring the elevation to be sustained over the window.
 */
public final class TimerFrequencyCheck extends AbstractWindowCheck {

  /** Expected movement packets per second at normal (1.0×) timer speed. */
  private static final double EXPECTED_PPS = 20.0D;

  /**
   * Maximum acceptable packets per second.  1.4× allows generous tolerance
   * for ViaVersion overhead, lag compensation, and normal burst variance.
   */
  private static final double MAX_PPS = EXPECTED_PPS * 1.45D; // 29 pps

  /** Time window (nanoseconds) for rate measurement. */
  private static final long WINDOW_NANOS = 1_000_000_000L; // 1 second

  private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();

  public TimerFrequencyCheck(int limit) {
    super(limit, 8);
  }

  @Override
  public String name() {
    return "TimerFrequency";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) {
      return CheckResult.clean(name(), category());
    }

    if (movement.intervalNanos() == Long.MAX_VALUE) {
      windows.remove(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    RateWindow window = windows.computeIfAbsent(movement.playerId(), k -> new RateWindow());
    double pps = window.record(movement.nanoTime());

    if (pps <= 0.0) {
      return CheckResult.clean(name(), category());
    }

    var ws = stats.record(movement.playerId(), pps);
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() > MAX_PPS) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Timer frequency elevated (mean="
                + String.format("%.1f", ws.mean())
                + "pps max="
                + String.format("%.1f", MAX_PPS)
                + "pps)",
            Math.min(1.0D, buf / 8.0D),
            false);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    windows.remove(playerId);
  }

  /** Sliding 1-second window for packet rate measurement. */
  private static final class RateWindow {
    private final long[] timestamps = new long[64];
    private int head = 0;
    private int count = 0;

    synchronized double record(long nowNanos) {
      timestamps[head % 64] = nowNanos;
      head++;
      count = Math.min(count + 1, 64);

      // Count packets within the last WINDOW_NANOS
      int recent = 0;
      for (int i = 0; i < count; i++) {
        if (nowNanos - timestamps[(head - 1 - i + 64) % 64] <= WINDOW_NANOS) {
          recent++;
        }
      }
      return recent; // packets in last 1 second ≈ packets per second
    }
  }
}
