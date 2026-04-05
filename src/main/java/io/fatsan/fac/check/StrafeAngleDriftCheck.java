package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StrafeAngleDriftCheck extends AbstractBufferedCheck {
  private final Map<String, Double> last = new ConcurrentHashMap<>();
private final Map<String, Integer> streak = new ConcurrentHashMap<>();
  public StrafeAngleDriftCheck(int limit) { super(limit); }
  @Override public String name() { return "StrafeAngleDrift"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) return CheckResult.clean(name(), category());
    if (movement.gliding() || movement.inVehicle()) return CheckResult.clean(name(), category());
    boolean trigger = false;
    Double prev=last.put(movement.playerId(), movement.deltaXZ()); if (prev!=null && Math.abs(prev-movement.deltaXZ())<0.005D && movement.deltaXZ()>0.35D) {int st=streak.getOrDefault(movement.playerId(),0)+1; streak.put(movement.playerId(),st); if(st>=6) trigger=true;} else {streak.put(movement.playerId(),0);}
    if (trigger) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "StrafeAngleDrift anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(movement.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
