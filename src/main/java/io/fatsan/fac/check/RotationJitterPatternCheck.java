package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.RotationEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RotationJitterPatternCheck extends AbstractBufferedCheck {
  private final Map<String, Float> lastYaw = new ConcurrentHashMap<>();

  public RotationJitterPatternCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "RotationJitterPattern";
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

    float current = rotation.deltaYaw();
    Float previous = lastYaw.put(rotation.playerId(), current);
    if (previous != null) {
      boolean alternating = Math.signum(previous) != Math.signum(current);
      boolean similarMagnitude = Math.abs(Math.abs(previous) - Math.abs(current)) < 3.0F;
      if (alternating && similarMagnitude && Math.abs(current) > 25.0F) {
        int buf = incrementBuffer(rotation.playerId());
        if (overLimit(buf)) {
          return new CheckResult(true, name(), category(), "Alternating high-amplitude yaw jitter pattern", Math.min(1.0D, buf / 8.0D), false);
        }
      } else {
        coolDown(rotation.playerId());
      }
    }

    return CheckResult.clean(name(), category());
  }
}
