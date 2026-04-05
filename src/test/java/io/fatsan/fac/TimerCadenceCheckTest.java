package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.TimerCadenceCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

class TimerCadenceCheckTest {
  @Test
  void shouldFlagFastCadence() {
    TimerCadenceCheck check = new TimerCadenceCheck(2);
    check.evaluate(new MovementEvent("player", System.nanoTime(), 0.2D, 0.0D, true, 0.0F, false, false, 4_000_000L));
    assertTrue(check.evaluate(new MovementEvent("player", System.nanoTime(), 0.2D, 0.0D, true, 0.0F, false, false, 4_000_000L)).suspicious());
  }
}
