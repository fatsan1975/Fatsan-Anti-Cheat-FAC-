package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

public final class MicroTeleportCheck extends AbstractBufferedCheck {
  public MicroTeleportCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "MicroTeleport";
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

    if (movement.inVehicle() || movement.gliding()) {
      coolDown(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    long intervalMs = movement.intervalNanos() / 1_000_000L;
    if (movement.onGround() && intervalMs > 0 && intervalMs <= 80L && movement.deltaXZ() > 2.25D) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Ground micro-teleport displacement pattern",
            Math.min(1.0D, buf / 8.0D),
            true);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
