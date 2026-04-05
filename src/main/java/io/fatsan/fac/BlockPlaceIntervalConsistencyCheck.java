package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockPlaceIntervalConsistencyCheck extends AbstractBufferedCheck {
  private final Map<String, Long> lastInterval = new ConcurrentHashMap<>();

  public BlockPlaceIntervalConsistencyCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "BlockPlaceIntervalConsistency";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockPlaceEventSignal place) || place.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    long current = place.intervalNanos();
    Long previous = lastInterval.put(place.playerId(), current);
    if (previous != null) {
      long diffMs = Math.abs(previous - current) / 1_000_000L;
      long intervalMs = current / 1_000_000L;
      if (intervalMs > 0 && intervalMs < 95L && diffMs <= 2L && place.horizontalSpeed() > 0.22D) {
        int buf = incrementBuffer(place.playerId());
        if (overLimit(buf)) {
          return new CheckResult(true, name(), category(), "Highly stable rapid block-place cadence while moving", Math.min(1.0D, buf / 8.0D), true);
        }
      } else {
        coolDown(place.playerId());
      }
    }

    return CheckResult.clean(name(), category());
  }
}
