package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import io.fatsan.fac.service.VelocityTracker;

/**
 * Detects horizontal-only KB cancel: player absorbs horizontal knockback
 * while accepting vertical component (goes up but not back).
 */
public final class VelocityHorizontalCheck extends AbstractBufferedCheck {
  private static final double MIN_HORIZONTAL_RATIO = 0.15;
  private static final double MIN_EXPECTED_HORIZONTAL_BPS = 2.0;
  private static final long MAX_KB_AGE_NANOS = 800_000_000L;

  private final VelocityTracker velocityTracker;

  public VelocityHorizontalCheck(int limit, VelocityTracker velocityTracker) {
    super(limit);
    this.velocityTracker = velocityTracker;
  }

  @Override public String name() { return "VelocityHorizontal"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof PlayerStateEvent state)) return CheckResult.clean(name(), category());

    VelocityTracker.PendingKnockback kb = velocityTracker.consumeKnockback(state.playerId());
    if (kb == null) return CheckResult.clean(name(), category());

    long age = System.nanoTime() - kb.timestamp();
    if (age > MAX_KB_AGE_NANOS) return CheckResult.clean(name(), category());

    double expectedHBps = kb.expectedHorizontal() * 20.0;
    if (expectedHBps < MIN_EXPECTED_HORIZONTAL_BPS) return CheckResult.clean(name(), category());

    double seconds = state.intervalNanos() / 1_000_000_000.0;
    if (seconds <= 0.0 || seconds > 0.2) return CheckResult.clean(name(), category());

    double observedHBps = state.deltaXZ() / seconds;
    double hRatio = observedHBps / expectedHBps;
    boolean verticalAccepted = Math.abs(state.deltaY()) > 0.05;

    if (hRatio < MIN_HORIZONTAL_RATIO && verticalAccepted) {
      int buf = incrementBuffer(state.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(),
            "Horizontal KB cancel (hRatio=" + String.format("%.2f", hRatio) + ")",
            Math.min(1.0, buf / 5.0), false);
      }
    } else {
      coolDown(state.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
