package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GroundAccelBurstCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> burst = new ConcurrentHashMap<>();
  public GroundAccelBurstCheck(int limit) { super(limit); }
  @Override public String name() { return "GroundAccelBurst"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent m) || !m.onGround() || m.inVehicle() || m.gliding()) return CheckResult.clean(name(), category());
    if (m.deltaXZ() > 0.95D && m.intervalNanos() < 60_000_000L) {
      int st = burst.getOrDefault(m.playerId(), 0) + 1; burst.put(m.playerId(), st);
      if (st >= 3) {
        int buf = incrementBuffer(m.playerId());
        if (overLimit(buf)) return new CheckResult(true, name(), category(), "Repeated ground acceleration burst pattern", Math.min(1.0D, buf / 8.0D), true);
      }
    } else { burst.put(m.playerId(), 0); coolDown(m.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
