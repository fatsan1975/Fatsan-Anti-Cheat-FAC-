package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.NoFallHeuristicCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

class NoFallHeuristicCheckTest {
  @Test
  void shouldFlagRepeatedGroundingWithoutFallTrace() {
    NoFallHeuristicCheck check = new NoFallHeuristicCheck(2);
    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.1D, -0.7D, false, 4.0F, false, false, 50_000_000L));
    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.1D, 0.0D, true, 0.0F, false, false, 50_000_000L));
    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.1D, -0.7D, false, 4.0F, false, false, 50_000_000L));
    assertTrue(check.evaluate(new MovementEvent("p", System.nanoTime(), 0.1D, 0.0D, true, 0.0F, false, false, 50_000_000L)).suspicious());
  }
}
