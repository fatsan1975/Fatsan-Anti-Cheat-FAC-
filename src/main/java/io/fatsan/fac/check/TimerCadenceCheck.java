package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

public final class TimerCadenceCheck extends AbstractBufferedCheck {
  public TimerCadenceCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "TimerCadence";
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

    long intervalMs = movement.intervalNanos() / 1_000_000L;
    if (intervalMs > 0 && intervalMs < 7) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(), "Movement cadence too fast", Math.min(1.0D, buf / 12.0D), true);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
