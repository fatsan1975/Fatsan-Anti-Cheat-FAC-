package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects BoatFly — riding a boat (or other vehicle) in mid-air with
 * impossible upward velocity.
 *
 * <p>Boats and minecarts are subject to gravity and cannot sustain positive
 * vertical displacement without external force.  A sustained positive mean
 * deltaY while {@code inVehicle=true} is a reliable indicator of vehicle-
 * based flight.
 *
 * <p>Note: vehicles can travel up small slopes (deltaY ≈ 0.2 per event
 * maximum on a 1-in-1 staircase), so the threshold is set above that.
 */
public final class BoatFlyCheck extends AbstractWindowCheck {

  /** Maximum mean deltaY (blocks/event) tolerated while in a vehicle. */
  private static final double MAX_VEHICLE_DELTA_Y = 0.3D;

  private static final double MAX_INTERVAL_SECONDS = 0.15D;

  public BoatFlyCheck(int limit) {
    super(limit, 5);
  }

  @Override
  public String name() {
    return "BoatFly";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement) || movement.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    if (!movement.inVehicle()) {
      // Reset window when player exits vehicle
      stats.clear(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    double seconds = movement.intervalNanos() / 1_000_000_000.0D;
    if (seconds <= 0.0D || seconds > MAX_INTERVAL_SECONDS) {
      return CheckResult.clean(name(), category());
    }

    var ws = stats.record(movement.playerId(), movement.deltaY());
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() > MAX_VEHICLE_DELTA_Y) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(),
            "Vehicle flight (meanDeltaY=" + String.format("%.3f", ws.mean()) + ")",
            Math.min(1.0D, buf / 5.0D),
            true);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
