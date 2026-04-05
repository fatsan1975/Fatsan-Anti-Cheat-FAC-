package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.PitchSnapCheck;
import io.fatsan.fac.model.RotationEvent;
import org.junit.jupiter.api.Test;

final class PitchSnapCheckTest {
  @Test
  void flagsHighPitchLowYawSnap() {
    PitchSnapCheck check = new PitchSnapCheck(2);
    check.evaluate(new RotationEvent("p", System.nanoTime(), 2.0F, 58.0F));
    assertTrue(check.evaluate(new RotationEvent("p", System.nanoTime(), 3.0F, -60.0F)).suspicious());
  }
}
