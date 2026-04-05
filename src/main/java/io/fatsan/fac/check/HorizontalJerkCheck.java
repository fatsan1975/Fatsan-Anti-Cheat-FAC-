package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HorizontalJerkCheck extends AbstractBufferedCheck {
  private final Map<String, Double> lastSpeed = new ConcurrentHashMap<>();

  public HorizontalJerkCheck(int limit) { super(limit); }
  @Override public String name() { return "HorizontalJerk"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent m) || m.intervalNanos() == Long.MAX_VALUE || m.gliding() || m.inVehicle()) return CheckResult.clean(name(), category());
    double s = m.intervalNanos() / 1_000_000_000.0D;
    if (s <= 0.0D || s > 0.15D) return CheckResult.clean(name(), category());
    double speed = m.deltaXZ() / s;
    Double last = lastSpeed.put(m.playerId(), speed);
    if (last != null && m.onGround() && (speed - last) > 8.5D) {
      int buf = incrementBuffer(m.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "Abrupt horizontal jerk spike", Math.min(1.0D, buf / 8.0D), true);
    } else coolDown(m.playerId());
    return CheckResult.clean(name(), category());
  }
}
