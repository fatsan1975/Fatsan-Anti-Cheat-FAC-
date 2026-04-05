package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

public final class ScaffoldPatternCheck extends AbstractBufferedCheck {
  public ScaffoldPatternCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "ScaffoldPattern";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockPlaceEventSignal place) || place.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    long intervalMs = place.intervalNanos() / 1_000_000L;
    if (place.sprinting() && place.horizontalSpeed() > 0.22D && intervalMs < 95) {
      int buf = incrementBuffer(place.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Fast sprint placement pattern",
            Math.min(1.0D, buf / 10.0D),
            true);
      }
    } else {
      coolDown(place.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
