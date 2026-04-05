package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.RotationJitterPatternCheck;
import io.fatsan.fac.model.RotationEvent;
import org.junit.jupiter.api.Test;

final class RotationJitterPatternCheckTest {
  @Test
  void flagsAlternatingYawJitterPattern() {
    RotationJitterPatternCheck check = new RotationJitterPatternCheck(2);
    check.evaluate(new RotationEvent("p", System.nanoTime(), 28.0F, 1.0F));
    check.evaluate(new RotationEvent("p", System.nanoTime(), -29.0F, 1.1F));
    assertTrue(check.evaluate(new RotationEvent("p", System.nanoTime(), 27.5F, 1.0F)).suspicious());
  }
}
