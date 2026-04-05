package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AirHoverStreakCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> hoverStreak = new ConcurrentHashMap<>();

  public AirHoverStreakCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "AirHoverStreak";
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

    boolean hovering = !movement.onGround() && !movement.gliding() && !movement.inVehicle() && Math.abs(movement.deltaY()) < 0.01D && movement.deltaXZ() > 0.08D;
    if (hovering) {
      int streak = hoverStreak.getOrDefault(movement.playerId(), 0) + 1;
      hoverStreak.put(movement.playerId(), streak);
      if (streak >= 6) {
        int buf = incrementBuffer(movement.playerId());
        if (overLimit(buf)) {
          return new CheckResult(true, name(), category(), "Sustained airborne hover-like movement streak", Math.min(1.0D, buf / 8.0D), true);
        }
      }
    } else {
      hoverStreak.put(movement.playerId(), 0);
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
