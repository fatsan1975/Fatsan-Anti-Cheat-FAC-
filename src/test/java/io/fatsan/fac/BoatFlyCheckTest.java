package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.BoatFlyCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class BoatFlyCheckTest {

  private static MovementEvent vehicle(double deltaY) {
    return new MovementEvent("p", System.nanoTime(), 0.3D, deltaY, false, 0F, false, true, 50_000_000L);
  }

  private static MovementEvent walking(double deltaY) {
    return new MovementEvent("p", System.nanoTime(), 0.3D, deltaY, true, 0F, false, false, 50_000_000L);
  }

  @Test
  void flagsSustainedAscentInVehicle() {
    BoatFlyCheck check = new BoatFlyCheck(3);
    for (int i = 0; i < 8; i++) check.evaluate(vehicle(0.5D));
    assertTrue(check.evaluate(vehicle(0.5D)).suspicious());
  }

  @Test
  void noFlagNormalBoatMovement() {
    BoatFlyCheck check = new BoatFlyCheck(2);
    for (int i = 0; i < 10; i++) assertFalse(check.evaluate(vehicle(0.0D)).suspicious());
  }

  @Test
  void noFlagWhenNotInVehicle() {
    BoatFlyCheck check = new BoatFlyCheck(1);
    for (int i = 0; i < 10; i++) assertFalse(check.evaluate(walking(0.5D)).suspicious());
  }

  @Test
  void resetsWindowOnExit() {
    BoatFlyCheck check = new BoatFlyCheck(2);
    for (int i = 0; i < 8; i++) check.evaluate(vehicle(0.5D));
    // Exit vehicle
    check.evaluate(walking(0.0D));
    // Re-enter: window reset, should not flag immediately
    assertFalse(check.evaluate(vehicle(0.5D)).suspicious());
  }
}
