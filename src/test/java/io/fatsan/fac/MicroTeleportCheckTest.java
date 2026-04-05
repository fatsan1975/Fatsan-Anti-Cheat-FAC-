package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.MicroTeleportCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class MicroTeleportCheckTest {
  @Test
  void flagsRepeatedGroundDisplacementSpikes() {
    MicroTeleportCheck check = new MicroTeleportCheck(2);

    check.evaluate(
        new MovementEvent(
            "player", System.nanoTime(), 2.30D, 0.0D, true, 0.0F, false, false, 60_000_000L));
    assertTrue(
        check.evaluate(
                new MovementEvent(
                    "player", System.nanoTime(), 2.40D, 0.0D, true, 0.0F, false, false, 60_000_000L))
            .suspicious());
  }

  @Test
  void ignoresExemptStates() {
    MicroTeleportCheck check = new MicroTeleportCheck(2);

    assertFalse(
        check.evaluate(
                new MovementEvent(
                    "player", System.nanoTime(), 2.50D, 0.0D, true, 0.0F, true, false, 60_000_000L))
            .suspicious());
  }
}
