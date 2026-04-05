package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

public final class ImpossibleGroundTransitionCheck extends AbstractBufferedCheck {
  public ImpossibleGroundTransitionCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "ImpossibleGroundTransition";
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

    if (movement.onGround()
        && movement.deltaY() > 0.52D
        && !movement.gliding()
        && !movement.inVehicle()) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Impossible upward grounded transition",
            Math.min(1.0D, buf / 6.0D),
            false);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
