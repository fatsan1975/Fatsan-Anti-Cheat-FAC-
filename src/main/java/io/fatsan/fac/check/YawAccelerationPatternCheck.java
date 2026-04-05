package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.RotationEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class YawAccelerationPatternCheck extends AbstractBufferedCheck {
  private final Map<String, Float> lastYaw = new ConcurrentHashMap<>();
  public YawAccelerationPatternCheck(int limit) { super(limit); }
  @Override public String name() { return "YawAccelerationPattern"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent r)) return CheckResult.clean(name(), category());
    Float prev = lastYaw.put(r.playerId(), r.deltaYaw());
    if (prev != null) {
      float accel = Math.abs(r.deltaYaw() - prev);
      if (accel > 70.0F && Math.abs(r.deltaYaw()) > 45.0F) {
        int buf = incrementBuffer(r.playerId());
        if (overLimit(buf)) return new CheckResult(true, name(), category(), "Yaw acceleration spike pattern", Math.min(1.0D, buf / 8.0D), false);
      } else coolDown(r.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
