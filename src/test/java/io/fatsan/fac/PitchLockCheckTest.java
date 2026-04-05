package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.PitchLockCheck;
import io.fatsan.fac.model.RotationEvent;
import org.junit.jupiter.api.Test;

final class PitchLockCheckTest {
  @Test
  void flagsRepeatedPitchLockPattern() {
    PitchLockCheck check = new PitchLockCheck(2);
    for (int i = 0; i < 6; i++) {
      check.evaluate(new RotationEvent("p", System.nanoTime(), 40.0F, 0.01F));
    }
    assertTrue(check.evaluate(new RotationEvent("p", System.nanoTime(), 42.0F, 0.02F)).suspicious());
  }
}
