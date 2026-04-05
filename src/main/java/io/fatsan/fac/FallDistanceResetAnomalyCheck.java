package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FallDistanceResetAnomalyCheck extends AbstractBufferedCheck {
  private final Map<String, Float> lastFall = new ConcurrentHashMap<>();
  public FallDistanceResetAnomalyCheck(int limit) { super(limit); }
  @Override public String name() { return "FallDistanceResetAnomaly"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent m)) return CheckResult.clean(name(), category());
    Float prev = lastFall.put(m.playerId(), m.fallDistance());
    if (prev != null && !m.onGround() && prev > 2.5F && m.fallDistance() < 0.2F && m.deltaY() < -0.08D) {
      int buf = incrementBuffer(m.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "Unexpected fall-distance reset while still airborne", Math.min(1.0D, buf / 8.0D), false);
    } else coolDown(m.playerId());
    return CheckResult.clean(name(), category());
  }
}
