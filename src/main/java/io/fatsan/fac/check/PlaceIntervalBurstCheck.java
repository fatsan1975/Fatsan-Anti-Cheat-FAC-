package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaceIntervalBurstCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public PlaceIntervalBurstCheck(int limit) { super(limit); }
  @Override public String name() { return "PlaceIntervalBurst"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockPlaceEventSignal place)) return CheckResult.clean(name(), category());
    if (place.intervalNanos() == Long.MAX_VALUE) return CheckResult.clean(name(), category());
    boolean trigger = false;
    long ms=place.intervalNanos()/1_000_000L; if(ms>0 && ms<55L && place.horizontalSpeed()>0.2D) trigger=true;
    if (trigger) {
      int buf = incrementBuffer(place.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "PlaceIntervalBurst anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(place.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
