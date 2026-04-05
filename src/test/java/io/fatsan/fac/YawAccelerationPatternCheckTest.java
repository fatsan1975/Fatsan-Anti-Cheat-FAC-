package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.YawAccelerationPatternCheck;
import io.fatsan.fac.model.RotationEvent;
import org.junit.jupiter.api.Test;

final class YawAccelerationPatternCheckTest {
  @Test
  void flagsYawAccelerationSpikes() {
    YawAccelerationPatternCheck check = new YawAccelerationPatternCheck(2);
    check.evaluate(new RotationEvent("p", System.nanoTime(), 10.0F, 1.0F));
    check.evaluate(new RotationEvent("p", System.nanoTime(), 95.0F, 1.0F));
    assertTrue(check.evaluate(new RotationEvent("p", System.nanoTime(), -90.0F, 1.0F)).suspicious());
  }
}
