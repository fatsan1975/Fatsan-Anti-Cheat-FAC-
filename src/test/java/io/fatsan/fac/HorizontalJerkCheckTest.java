package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.HorizontalJerkCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class HorizontalJerkCheckTest {
  @Test
  void flagsAbruptHorizontalJerkSpike() {
    HorizontalJerkCheck check = new HorizontalJerkCheck(1);
    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.1D, 0.0D, true, 0.0F, false, false, 50_000_000L));
    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.9D, 0.0D, true, 0.0F, false, false, 50_000_000L));
    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.1D, 0.0D, true, 0.0F, false, false, 50_000_000L));
    assertTrue(check.evaluate(new MovementEvent("p", System.nanoTime(), 1.0D, 0.0D, true, 0.0F, false, false, 50_000_000L)).suspicious());
  }
}
