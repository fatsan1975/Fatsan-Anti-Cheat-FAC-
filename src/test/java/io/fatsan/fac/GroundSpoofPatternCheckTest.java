package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.GroundSpoofPatternCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class GroundSpoofPatternCheckTest {
  @Test
  void flagsImpossibleGroundFallPattern() {
    GroundSpoofPatternCheck check = new GroundSpoofPatternCheck(2);
    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.1D, -0.5D, true, 2.5F, false, false, 50_000_000L));
    assertTrue(check.evaluate(new MovementEvent("p", System.nanoTime(), 0.1D, -0.45D, true, 2.2F, false, false, 50_000_000L)).suspicious());
  }
}
