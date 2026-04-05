package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.RotationDirectionLockCheck;
import io.fatsan.fac.model.RotationEvent;
import org.junit.jupiter.api.Test;

final class RotationDirectionLockCheckTest {
  @Test
  void flagsSustainedSingleDirectionYawBursts() {
    RotationDirectionLockCheck check = new RotationDirectionLockCheck(1);

    for (int i = 0; i < 12; i++) {
      check.evaluate(new RotationEvent("p", System.nanoTime(), 20.0F, 0.8F));
    }

    assertTrue(check.evaluate(new RotationEvent("p", System.nanoTime(), 21.0F, 0.7F)).suspicious());
  }
}
