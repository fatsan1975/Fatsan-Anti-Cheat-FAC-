package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.LowGravityPatternCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class LowGravityPatternCheckTest {
  @Test
  void flagsLowGravityDescentPattern() {
    LowGravityPatternCheck check = new LowGravityPatternCheck(2);
    for (int i = 0; i < 7; i++) {
      check.evaluate(new MovementEvent("p", System.nanoTime(), 0.2D, -0.02D, false, 1.5F, false, false, 50_000_000L));
    }
    assertTrue(check.evaluate(new MovementEvent("p", System.nanoTime(), 0.2D, -0.015D, false, 1.8F, false, false, 50_000_000L)).suspicious());
  }
}
