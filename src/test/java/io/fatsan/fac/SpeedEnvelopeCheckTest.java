package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.SpeedEnvelopeCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class SpeedEnvelopeCheckTest {
  @Test
  void flagsSustainedGroundSpeedEnvelopeViolations() {
    SpeedEnvelopeCheck check = new SpeedEnvelopeCheck(2);

    check.evaluate(
        new MovementEvent("player", System.nanoTime(), 0.95D, 0.0D, true, 0.0F, false, false, 50_000_000L));
    assertTrue(
        check.evaluate(
                new MovementEvent(
                    "player", System.nanoTime(), 0.90D, 0.0D, true, 0.0F, false, false, 50_000_000L))
            .suspicious());
  }

  @Test
  void ignoresNormalOrExemptMovementStates() {
    SpeedEnvelopeCheck check = new SpeedEnvelopeCheck(2);

    assertFalse(
        check.evaluate(
                new MovementEvent(
                    "player", System.nanoTime(), 0.22D, 0.0D, true, 0.0F, false, false, 50_000_000L))
            .suspicious());

    assertFalse(
        check.evaluate(
                new MovementEvent(
                    "player", System.nanoTime(), 1.10D, 0.0D, true, 0.0F, true, false, 50_000_000L))
            .suspicious());
  }
}
