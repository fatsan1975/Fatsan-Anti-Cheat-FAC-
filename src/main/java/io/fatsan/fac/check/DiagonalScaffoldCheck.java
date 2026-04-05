package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects diagonal scaffolding: cheat clients that bridge diagonally
 * while sprinting at impossible speed. Normal players can only bridge
 * straight or sneak-edge for diagonal, not sprint-scaffold diagonally.
 */
public final class DiagonalScaffoldCheck extends AbstractBufferedCheck {
  private static final double DIAGONAL_SPEED_THRESHOLD = 0.28;
  private static final long MAX_DIAGONAL_INTERVAL_MS = 80;

  public DiagonalScaffoldCheck(int limit) { super(limit); }

  @Override public String name() { return "DiagonalScaffold"; }
  @Override public CheckCategory category() { return CheckCategory.WORLD; }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockPlaceEventSignal place) || place.intervalNanos() == Long.MAX_VALUE)
      return CheckResult.clean(name(), category());

    long intervalMs = place.intervalNanos() / 1_000_000L;
    if (place.sprinting() && place.horizontalSpeed() > DIAGONAL_SPEED_THRESHOLD && intervalMs < MAX_DIAGONAL_INTERVAL_MS) {
      int buf = incrementBuffer(place.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(),
            "Diagonal scaffold (speed=" + String.format("%.2f", place.horizontalSpeed()) + " interval=" + intervalMs + "ms)",
            Math.min(1.0, buf / 8.0), true);
      }
    } else {
      coolDown(place.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
