package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;

public final class ImpossibleCriticalCheck extends AbstractBufferedCheck {
  public ImpossibleCriticalCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "ImpossibleCritical";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) {
      return CheckResult.clean(name(), category());
    }

    boolean impossibleCritical =
        hit.criticalLike()
            && (hit.onGround() || hit.fallDistance() < 0.08F || hit.gliding() || hit.inVehicle());

    if (impossibleCritical) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Critical-like hit with impossible movement state",
            Math.min(1.0D, buf / 7.0D),
            true);
      }
    } else {
      coolDown(hit.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
