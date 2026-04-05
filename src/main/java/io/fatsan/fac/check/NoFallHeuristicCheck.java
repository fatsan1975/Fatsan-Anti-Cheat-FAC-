package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NoFallHeuristicCheck extends AbstractBufferedCheck {
  private final Map<String, Boolean> falling = new ConcurrentHashMap<>();

  public NoFallHeuristicCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "NoFallHeuristic";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) {
      return CheckResult.clean(name(), category());
    }

    if (!movement.onGround() && movement.deltaY() < -0.45D) {
      falling.put(movement.playerId(), true);
      return CheckResult.clean(name(), category());
    }

    if (movement.onGround() && falling.getOrDefault(movement.playerId(), false)) {
      falling.put(movement.playerId(), false);
      if (movement.fallDistance() < 0.15F && !movement.gliding() && !movement.inVehicle()) {
        int buf = incrementBuffer(movement.playerId());
        if (overLimit(buf)) {
          return new CheckResult(
              true,
              name(),
              category(),
              "Grounded without expected fall trace",
              Math.min(1.0D, buf / 8.0D),
              true);
        }
      } else {
        coolDown(movement.playerId());
      }
    }

    return CheckResult.clean(name(), category());
  }
}
