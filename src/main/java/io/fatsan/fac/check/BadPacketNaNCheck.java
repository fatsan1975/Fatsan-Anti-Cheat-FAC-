package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

public final class BadPacketNaNCheck implements Check {
  @Override
  public String name() {
    return "BadPacketNaN";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (event instanceof MovementEvent movement && !Double.isFinite(movement.deltaXZ())) {
      return new CheckResult(true, name(), category(), "Non-finite movement delta", 1.0D, true);
    }
    return CheckResult.clean(name(), category());
  }
}
