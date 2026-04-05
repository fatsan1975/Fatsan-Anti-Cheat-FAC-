package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

public final class VerticalMotionEnvelopeCheck extends AbstractBufferedCheck {
  private static final double HARD_UPWARD_SPEED_BPS = 13.0D;

  public VerticalMotionEnvelopeCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "VerticalMotionEnvelope";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement) || movement.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    if (movement.onGround() || movement.inVehicle() || movement.gliding()) {
      coolDown(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    double seconds = movement.intervalNanos() / 1_000_000_000.0D;
    if (seconds <= 0.0D || seconds > 0.15D || movement.deltaY() <= 0.0D) {
      return CheckResult.clean(name(), category());
    }

    double upwardBps = movement.deltaY() / seconds;
    if (upwardBps > HARD_UPWARD_SPEED_BPS) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Vertical ascent exceeded conservative movement envelope",
            Math.min(1.0D, buf / 8.0D),
            true);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
