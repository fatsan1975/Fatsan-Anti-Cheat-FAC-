package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.ImpossibleGroundTransitionCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

class ImpossibleGroundTransitionCheckTest {
  @Test
  void shouldFlagRepeatedImpossibleGroundJumps() {
    ImpossibleGroundTransitionCheck check = new ImpossibleGroundTransitionCheck(2);
    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.2D, 0.7D, true, 0.0F, false, false, 50_000_000L));
    assertTrue(
        check
            .evaluate(new MovementEvent("p", System.nanoTime(), 0.2D, 0.8D, true, 0.0F, false, false, 50_000_000L))
            .suspicious());
  }
}
