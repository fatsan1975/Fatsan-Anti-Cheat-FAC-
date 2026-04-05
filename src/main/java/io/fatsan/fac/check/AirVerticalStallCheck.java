package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AirVerticalStallCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> stallTicks = new ConcurrentHashMap<>();

  public AirVerticalStallCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "AirVerticalStall";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) return CheckResult.clean(name(), category());
    if (movement.onGround() || movement.gliding() || movement.inVehicle()) {
      stallTicks.put(movement.playerId(), 0);
      return CheckResult.clean(name(), category());
    }

    boolean nearZeroVertical = Math.abs(movement.deltaY()) <= 0.0035D;
    int ticks = nearZeroVertical ? stallTicks.getOrDefault(movement.playerId(), 0) + 1 : 0;
    stallTicks.put(movement.playerId(), ticks);

    boolean suspicious = ticks >= 7 && movement.deltaXZ() > 0.07D;
    if (suspicious) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(), "airborne vertical stall pattern", Math.min(1.0D, buf / 6.0D), false);
      }
    } else {
      coolDown(movement.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
