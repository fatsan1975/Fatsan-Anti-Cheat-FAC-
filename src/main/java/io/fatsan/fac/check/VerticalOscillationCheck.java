package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VerticalOscillationCheck extends AbstractBufferedCheck {
  private final Map<String, Double> last = new ConcurrentHashMap<>();
  public VerticalOscillationCheck(int limit) { super(limit); }
  @Override public String name() { return "VerticalOscillation"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) return CheckResult.clean(name(), category());
    if (movement.onGround() || movement.gliding() || movement.inVehicle()) return CheckResult.clean(name(), category());
    boolean trigger = false;
    Double prev=last.put(movement.playerId(), movement.deltaY()); if (prev!=null && Math.signum(prev)!=Math.signum(movement.deltaY()) && Math.abs(prev)>0.28D && Math.abs(movement.deltaY())>0.28D) trigger=true;
    if (trigger) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "VerticalOscillation anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(movement.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
