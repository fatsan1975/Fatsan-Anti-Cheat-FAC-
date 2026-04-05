package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.RotationYawVarianceCollapseCheck;
import io.fatsan.fac.model.RotationEvent;
import org.junit.jupiter.api.Test;

final class RotationYawVarianceCollapseCheckTest {
  @Test
  void flagsYawVarianceCollapsePattern() {
    RotationYawVarianceCollapseCheck check = new RotationYawVarianceCollapseCheck(1);
    for (int i = 0; i < 9; i++) {
      check.evaluate(new RotationEvent("p", System.nanoTime(), 9.45F, 0.7F));
    }
    assertTrue(check.evaluate(new RotationEvent("p", System.nanoTime(), 9.46F, 0.6F)).suspicious());
  }
}
