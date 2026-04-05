package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.MovementGroundSpeedPlateauCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class MovementGroundSpeedPlateauCheckTest {
  @Test
  void flagsGroundPlateauSpeedPattern() {
    MovementGroundSpeedPlateauCheck check = new MovementGroundSpeedPlateauCheck(1);
    for (int i = 0; i < 10; i++) {
      check.evaluate(new MovementEvent("p", System.nanoTime(), 0.31D, 0.0D, true, 0.0F, false, false, 50_000_000L));
    }
    assertTrue(check.evaluate(new MovementEvent("p", System.nanoTime(), 0.311D, 0.0D, true, 0.0F, false, false, 50_000_000L)).suspicious());
  }
}
