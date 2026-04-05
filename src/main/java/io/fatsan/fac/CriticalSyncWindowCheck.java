package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CriticalSyncWindowCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> streak = new ConcurrentHashMap<>();

  public CriticalSyncWindowCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "CriticalSyncWindow";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) return CheckResult.clean(name(), category());
    if (hit.intervalNanos() == Long.MAX_VALUE || hit.inVehicle() || hit.gliding()) {
      return CheckResult.clean(name(), category());
    }

    long ms = hit.intervalNanos() / 1_000_000L;
    boolean tightCritical = hit.criticalLike() && !hit.onGround() && hit.fallDistance() > 0.05F && ms > 40L && ms < 130L;

    int s = tightCritical ? streak.getOrDefault(hit.playerId(), 0) + 1 : 0;
    streak.put(hit.playerId(), s);

    if (s >= 6) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(), "critical timing window lock", Math.min(1.0D, buf / 7.0D), false);
      }
    } else {
      coolDown(hit.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
