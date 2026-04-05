package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import io.fatsan.fac.service.VelocityTracker;

/**
 * Detects vertical-only KB cancel: player absorbs vertical knockback
 * while accepting horizontal (slides back but doesn't go up).
 */
public final class VelocityVerticalCheck extends AbstractBufferedCheck {
  private static final double MIN_VERTICAL_THRESHOLD = 0.05;
  private static final double MIN_EXPECTED_VY = 0.2;
  private static final long MAX_KB_AGE_NANOS = 800_000_000L;

  private final VelocityTracker velocityTracker;

  public VelocityVerticalCheck(int limit, VelocityTracker velocityTracker) {
    super(limit);
    this.velocityTracker = velocityTracker;
  }

  @Override public String name() { return "VelocityVertical"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof PlayerStateEvent state)) return CheckResult.clean(name(), category());

    VelocityTracker.PendingKnockback kb = velocityTracker.consumeKnockback(state.playerId());
    if (kb == null) return CheckResult.clean(name(), category());

    long age = System.nanoTime() - kb.timestamp();
    if (age > MAX_KB_AGE_NANOS) return CheckResult.clean(name(), category());
    if (kb.expectedVy() < MIN_EXPECTED_VY) return CheckResult.clean(name(), category());

    double seconds = state.intervalNanos() / 1_000_000_000.0;
    if (seconds <= 0.0 || seconds > 0.2) return CheckResult.clean(name(), category());

    boolean verticalCancelled = Math.abs(state.deltaY()) < MIN_VERTICAL_THRESHOLD;
    boolean horizontalAccepted = state.deltaXZ() > 0.1;

    if (verticalCancelled && horizontalAccepted) {
      int buf = incrementBuffer(state.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(),
            "Vertical KB cancel (dy=" + String.format("%.3f", state.deltaY())
                + " expectedVy=" + String.format("%.2f", kb.expectedVy()) + ")",
            Math.min(1.0, buf / 5.0), false);
      }
    } else {
      coolDown(state.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
