package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.RotationEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PitchLockCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> lockStreak = new ConcurrentHashMap<>();

  public PitchLockCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "PitchLock";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent rotation)) {
      return CheckResult.clean(name(), category());
    }

    boolean locked = Math.abs(rotation.deltaPitch()) < 0.05F && Math.abs(rotation.deltaYaw()) > 30.0F;
    if (locked) {
      int streak = lockStreak.getOrDefault(rotation.playerId(), 0) + 1;
      lockStreak.put(rotation.playerId(), streak);
      if (streak >= 5) {
        int buf = incrementBuffer(rotation.playerId());
        if (overLimit(buf)) {
          return new CheckResult(true, name(), category(), "Repeated high-yaw with near-zero pitch lock", Math.min(1.0D, buf / 8.0D), false);
        }
      }
    } else {
      lockStreak.put(rotation.playerId(), 0);
      coolDown(rotation.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
