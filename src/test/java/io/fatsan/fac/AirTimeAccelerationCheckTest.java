package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.AirTimeAccelerationCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class AirTimeAccelerationCheckTest {
  @Test
  void flagsUnnaturalAirborneAccelerationSpike() {
    AirTimeAccelerationCheck check = new AirTimeAccelerationCheck(1);

    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.30D, 0.01D, false, 0.0F, false, false, 50_000_000L));
    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.34D, 0.01D, false, 0.0F, false, false, 50_000_000L));

    assertTrue(
        check.evaluate(new MovementEvent("p", System.nanoTime(), 0.85D, 0.01D, false, 0.0F, false, false, 50_000_000L)).suspicious());
  }
}
