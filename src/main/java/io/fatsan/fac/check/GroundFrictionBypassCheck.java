package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GroundFrictionBypassCheck extends AbstractBufferedCheck {
  private final Map<String, Double> last = new ConcurrentHashMap<>();
  public GroundFrictionBypassCheck(int limit) { super(limit); }
  @Override public String name() { return "GroundFrictionBypass"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) return CheckResult.clean(name(), category());
    if (!movement.onGround() || movement.gliding() || movement.inVehicle()) return CheckResult.clean(name(), category());
    boolean trigger = false;
    Double prev=last.put(movement.playerId(), movement.deltaXZ()); if (prev!=null && prev>0.45D && movement.deltaXZ()>0.44D) trigger=true;
    if (trigger) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "GroundFrictionBypass anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(movement.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
