package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.StepCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class StepCheckTest {

  private static MovementEvent grounded(double deltaY, double speedBps) {
    double seconds = 0.05D;
    return new MovementEvent("p", System.nanoTime(), speedBps * seconds, deltaY, true, 0F, false, false, (long)(seconds * 1e9));
  }

  @Test
  void flagsOverVanillaStepHeight() {
    StepCheck check = new StepCheck(2);
    check.evaluate(grounded(1.0D, 3.0D));
    check.evaluate(grounded(1.0D, 3.0D));
    assertTrue(check.evaluate(grounded(1.0D, 3.0D)).suspicious());
  }

  @Test
  void noFlagVanillaStep() {
    StepCheck check = new StepCheck(1);
    for (int i = 0; i < 10; i++) {
      assertFalse(check.evaluate(grounded(0.5D, 3.0D)).suspicious());
    }
  }

  @Test
  void noFlagWhenAirborne() {
    StepCheck check = new StepCheck(1);
    var air = new MovementEvent("p", System.nanoTime(), 0.15D, 1.0D, false, 0F, false, false, 50_000_000L);
    assertFalse(check.evaluate(air).suspicious());
  }

  @Test
  void noFlagLowSpeed() {
    StepCheck check = new StepCheck(1);
    // deltaY=1.0 suspicious but speed < MIN_LATERAL_BPS=1.5
    for (int i = 0; i < 5; i++) {
      assertFalse(check.evaluate(grounded(1.0D, 0.5D)).suspicious());
    }
  }
}
