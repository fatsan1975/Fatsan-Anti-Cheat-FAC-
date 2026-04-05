package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects delayed KB bypass: cheat clients receive KB but delay applying
 * it for 600ms+ (beyond the normal window the server expects).
 */
public final class VelocityDelayedCheck extends AbstractBufferedCheck {
  private static final long KB_DEADLINE_NANOS = 600_000_000L;
  private static final double MIN_RESPONSE_DELTA = 0.1;

  private final Map<String, Long> pendingKB = new ConcurrentHashMap<>();
  private final Map<String, Boolean> responded = new ConcurrentHashMap<>();

  public VelocityDelayedCheck(int limit) {
    super(limit);
  }

  @Override public String name() { return "VelocityDelayed"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  public void recordKnockback(String playerId) {
    pendingKB.put(playerId, System.nanoTime());
    responded.put(playerId, false);
  }

  void recordKnockbackWithTimestamp(String playerId, long nanoTime) {
    pendingKB.put(playerId, nanoTime);
    responded.put(playerId, false);
  }

  public void recordKnockbackResponse(String playerId) {
    responded.put(playerId, true);
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof PlayerStateEvent state)) return CheckResult.clean(name(), category());

    Long kbTime = pendingKB.get(state.playerId());
    if (kbTime == null) return CheckResult.clean(name(), category());

    if (state.deltaXZ() > MIN_RESPONSE_DELTA || Math.abs(state.deltaY()) > MIN_RESPONSE_DELTA) {
      responded.put(state.playerId(), true);
    }

    long elapsed = System.nanoTime() - kbTime;
    if (elapsed > KB_DEADLINE_NANOS) {
      Boolean hasResponded = responded.getOrDefault(state.playerId(), false);
      pendingKB.remove(state.playerId());
      responded.remove(state.playerId());

      if (!hasResponded) {
        int buf = incrementBuffer(state.playerId());
        if (overLimit(buf)) {
          return new CheckResult(true, name(), category(),
              "Delayed KB response (elapsed=" + String.format("%.0f", elapsed / 1_000_000.0) + "ms noResponse=true)",
              Math.min(1.0, buf / 5.0), false);
        }
      }
    }
    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    pendingKB.remove(playerId);
    responded.remove(playerId);
  }
}
