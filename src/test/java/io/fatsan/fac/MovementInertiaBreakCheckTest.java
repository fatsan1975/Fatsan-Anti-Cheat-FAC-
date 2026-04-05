package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.MovementInertiaBreakCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class MovementInertiaBreakCheckTest {

  private static MovementEvent airborne(String id, double deltaXZ) {
    return new MovementEvent(id, System.nanoTime(), deltaXZ, 0.0D, false, 0.0F, false, false, 50_000_000L);
  }

  private static MovementEvent grounded(String id, double deltaXZ) {
    return new MovementEvent(id, System.nanoTime(), deltaXZ, 0.0D, true, 0.0F, false, false, 50_000_000L);
  }

  @Test
  void flagsAbruptAirborneStop() {
    MovementInertiaBreakCheck check = new MovementInertiaBreakCheck(2);

    // Build a sustained-movement baseline: mean deltaXZ ~ 0.25 (> MIN_BASELINE_DELTA=0.18)
    check.evaluate(airborne("p", 0.25D));
    check.evaluate(airborne("p", 0.25D));
    check.evaluate(airborne("p", 0.25D));

    // Abrupt stop (deltaXZ < 0.02): triggers inertia break detection.
    // First stop increments buffer; second triggers overLimit.
    check.evaluate(airborne("p", 0.01D));
    assertTrue(check.evaluate(airborne("p", 0.01D)).suspicious());
  }

  @Test
  void noFlagWhenBaselineIsTooLow() {
    MovementInertiaBreakCheck check = new MovementInertiaBreakCheck(1);

    // Baseline below MIN_BASELINE_DELTA (0.18) → stop is not an inertia break
    check.evaluate(airborne("p", 0.10D));
    check.evaluate(airborne("p", 0.10D));
    check.evaluate(airborne("p", 0.10D));
    assertFalse(check.evaluate(airborne("p", 0.01D)).suspicious());
  }

  @Test
  void noFlagWhenGrounded() {
    MovementInertiaBreakCheck check = new MovementInertiaBreakCheck(1);

    check.evaluate(airborne("p", 0.25D));
    check.evaluate(airborne("p", 0.25D));
    check.evaluate(airborne("p", 0.25D));

    // When onGround=true the check should not flag
    assertFalse(check.evaluate(grounded("p", 0.01D)).suspicious());
  }

  @Test
  void noFlagWhenGlidingOrInVehicle() {
    MovementInertiaBreakCheck check = new MovementInertiaBreakCheck(1);

    // Gliding player — exempt
    MovementEvent glide = new MovementEvent("p", System.nanoTime(), 0.01D, -0.2D, false, 0.0F, true, false, 50_000_000L);
    assertFalse(check.evaluate(glide).suspicious());

    // Vehicle rider — exempt
    MovementEvent vehicle = new MovementEvent("p", System.nanoTime(), 0.01D, 0.0D, false, 0.0F, false, true, 50_000_000L);
    assertFalse(check.evaluate(vehicle).suspicious());
  }
}
