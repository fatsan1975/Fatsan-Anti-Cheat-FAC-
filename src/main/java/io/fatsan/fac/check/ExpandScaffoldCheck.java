package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects expand/extend scaffold: placing blocks at reach distance while
 * moving at high speed. Normal bridging requires being close to the edge;
 * expand scaffolds place blocks 3-4 blocks away while sprinting.
 */
public final class ExpandScaffoldCheck extends AbstractBufferedCheck {
  private static final double EXPAND_SPEED_THRESHOLD = 0.42;
  private static final long MAX_EXPAND_INTERVAL_MS = 70;

  public ExpandScaffoldCheck(int limit) { super(limit); }

  @Override public String name() { return "ExpandScaffold"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockPlaceEventSignal place) || place.intervalNanos() == Long.MAX_VALUE)
      return CheckResult.clean(name(), category());

    long intervalMs = place.intervalNanos() / 1_000_000L;
    if (place.sprinting() && place.horizontalSpeed() > EXPAND_SPEED_THRESHOLD && intervalMs < MAX_EXPAND_INTERVAL_MS) {
      int buf = incrementBuffer(place.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(),
            "Expand scaffold (speed=" + String.format("%.2f", place.horizontalSpeed()) + " interval=" + intervalMs + "ms)",
            Math.min(1.0, buf / 8.0), true);
      }
    } else {
      coolDown(place.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
