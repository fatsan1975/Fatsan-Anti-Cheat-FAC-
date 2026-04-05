package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.RotationEvent;

public final class CombatRotationSnapCheck extends AbstractBufferedCheck {
  public CombatRotationSnapCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "CombatRotationSnap";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent rotation)) {
      return CheckResult.clean(name(), category());
    }

    float yaw = Math.abs(rotation.deltaYaw());
    float pitch = Math.abs(rotation.deltaPitch());
    if (yaw > 120.0F && pitch < 5.0F) {
      int buf = incrementBuffer(rotation.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(), "Snap rotation pattern", Math.min(1.0D, buf / 10.0D), false);
      }
    } else {
      coolDown(rotation.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
