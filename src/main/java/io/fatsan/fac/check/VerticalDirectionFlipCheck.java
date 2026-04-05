package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VerticalDirectionFlipCheck extends AbstractBufferedCheck {
  private final Map<String, Double> lastDeltaY = new ConcurrentHashMap<>();

  public VerticalDirectionFlipCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "VerticalDirectionFlip";
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

    if (movement.onGround() || movement.gliding() || movement.inVehicle()) {
      lastDeltaY.remove(movement.playerId());
      coolDown(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    double current = movement.deltaY();
    Double previous = lastDeltaY.put(movement.playerId(), current);
    if (previous == null) {
      return CheckResult.clean(name(), category());
    }

    long intervalMs = movement.intervalNanos() / 1_000_000L;
    boolean flip = Math.signum(previous) != Math.signum(current);
    if (flip && Math.abs(previous) > 0.42D && Math.abs(current) > 0.42D && intervalMs <= 80L) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Rapid high-amplitude vertical direction flips",
            Math.min(1.0D, buf / 8.0D),
            false);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
