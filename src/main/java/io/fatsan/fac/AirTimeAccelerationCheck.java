package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AirTimeAccelerationCheck extends AbstractBufferedCheck {
  private final Map<String, Double> last = new ConcurrentHashMap<>();
  public AirTimeAccelerationCheck(int limit) { super(limit); }
  @Override public String name() { return "AirTimeAcceleration"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) return CheckResult.clean(name(), category());
    if (movement.intervalNanos() == Long.MAX_VALUE || movement.onGround() || movement.gliding() || movement.inVehicle()) return CheckResult.clean(name(), category());
    boolean trigger = false;
    double s=movement.intervalNanos()/1_000_000_000.0D; if (s<=0.0D || s>0.15D) return CheckResult.clean(name(), category()); double speed=movement.deltaXZ()/s; Double prev=last.put(movement.playerId(), speed); if (prev!=null && speed-prev>5.5D && speed>9.0D) trigger=true;
    if (trigger) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "AirTimeAcceleration anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(movement.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
