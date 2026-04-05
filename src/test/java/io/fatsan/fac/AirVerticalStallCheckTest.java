package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.AirVerticalStallCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class AirVerticalStallCheckTest {
  @Test
  void flagsRepeatedNearZeroVerticalAirTicks() {
    AirVerticalStallCheck check = new AirVerticalStallCheck(1);
    for (int i = 0; i < 10; i++) {
      check.evaluate(new MovementEvent("p", System.nanoTime(), 0.12D, 0.001D, false, 0.0F, false, false, 50_000_000L));
    }
    assertTrue(check.evaluate(new MovementEvent("p", System.nanoTime(), 0.11D, 0.001D, false, 0.0F, false, false, 50_000_000L)).suspicious());
  }
}
