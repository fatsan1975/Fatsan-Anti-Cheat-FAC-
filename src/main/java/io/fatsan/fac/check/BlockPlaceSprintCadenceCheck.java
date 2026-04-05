package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockPlaceSprintCadenceCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> streak = new ConcurrentHashMap<>();

  public BlockPlaceSprintCadenceCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "BlockPlaceSprintCadence";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockPlaceEventSignal place)) return CheckResult.clean(name(), category());
    if (place.intervalNanos() == Long.MAX_VALUE) return CheckResult.clean(name(), category());

    long ms = place.intervalNanos() / 1_000_000L;
    boolean suspiciousPattern = place.sprinting() && place.horizontalSpeed() > 0.28D && ms >= 30L && ms <= 60L;
    int s = suspiciousPattern ? streak.getOrDefault(place.playerId(), 0) + 1 : 0;
    streak.put(place.playerId(), s);

    if (s >= 7) {
      int buf = incrementBuffer(place.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(), "sprint-place cadence lock", Math.min(1.0D, buf / 7.0D), false);
      }
    } else {
      coolDown(place.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
