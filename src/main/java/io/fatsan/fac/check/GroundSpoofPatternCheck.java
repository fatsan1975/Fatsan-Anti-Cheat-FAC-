package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

public final class GroundSpoofPatternCheck extends AbstractBufferedCheck {
  public GroundSpoofPatternCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "GroundSpoofPattern";
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

    boolean impossibleGround = movement.onGround() && movement.fallDistance() > 2.0F && movement.deltaY() < -0.35D;
    if (impossibleGround) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(), "Ground state conflicts with fall kinematics", Math.min(1.0D, buf / 8.0D), false);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
