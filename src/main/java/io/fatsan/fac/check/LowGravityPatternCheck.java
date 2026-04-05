package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LowGravityPatternCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> streak = new ConcurrentHashMap<>();
  public LowGravityPatternCheck(int limit) { super(limit); }
  @Override public String name() { return "LowGravityPattern"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent m) || m.onGround() || m.gliding() || m.inVehicle()) return CheckResult.clean(name(), category());
    boolean lowG = m.deltaY() < 0.0D && m.deltaY() > -0.03D && m.fallDistance() > 1.2F;
    if (lowG) {
      int st = streak.getOrDefault(m.playerId(), 0) + 1; streak.put(m.playerId(), st);
      if (st >= 6) {
        int buf = incrementBuffer(m.playerId());
        if (overLimit(buf)) return new CheckResult(true, name(), category(), "Airborne descent resembles low-gravity profile", Math.min(1.0D, buf / 8.0D), true);
      }
    } else { streak.put(m.playerId(), 0); coolDown(m.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
