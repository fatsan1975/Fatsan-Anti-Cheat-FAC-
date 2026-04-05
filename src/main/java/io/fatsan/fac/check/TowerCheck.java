package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects Tower — the scaffold variant where a player places blocks directly
 * beneath themselves to build a vertical pillar at impossible speed.
 *
 * <p>Tower hack places blocks at a rate far exceeding human capability
 * (one block per game tick = 50ms).  The key distinction from horizontal
 * scaffold: the player is nearly stationary horizontally while placing
 * blocks in extremely rapid succession.
 *
 * <p>Detection: sustained very-short block-place intervals combined with
 * near-zero horizontal movement speed.  Legitimate players building a tower
 * manually cannot exceed ~4 blocks/second without a macro.
 */
public final class TowerCheck extends AbstractWindowCheck {

  /**
   * Maximum mean interval (nanoseconds) between block placements to be
   * considered a tower pattern.  200ms = 5 blocks/second, well above human.
   */
  private static final double MAX_TOWER_INTERVAL_NS = 200_000_000D;

  /**
   * Maximum horizontal speed (blocks/tick proxy from BlockPlaceEventSignal)
   * while tower-placing.  Tower is vertical; moving fast horizontally = scaffold.
   */
  private static final double MAX_HORIZONTAL_SPEED = 0.15D;

  public TowerCheck(int limit) {
    super(limit, 6);
  }

  @Override
  public String name() {
    return "Tower";
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

    // High horizontal speed = likely scaffold not tower
    if (place.horizontalSpeed() > MAX_HORIZONTAL_SPEED) {
      coolDown(place.playerId());
      return CheckResult.clean(name(), category());
    }

    var ws = stats.record(place.playerId(), place.intervalNanos());
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    // Mean interval below threshold with uniform cadence = tower macro
    if (ws.mean() < MAX_TOWER_INTERVAL_NS && ws.isUniformlyCadenced(0.08)) {
      int buf = incrementBuffer(place.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(),
            "Tower placement (meanInterval=" + String.format("%.0f", ws.mean() / 1_000_000D) + "ms"
                + " speed=" + String.format("%.2f", place.horizontalSpeed()) + ")",
            Math.min(1.0D, buf / 5.0D),
            true);
      }
    } else {
      coolDown(place.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
