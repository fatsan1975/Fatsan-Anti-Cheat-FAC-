package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.VerticalMotionEnvelopeCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class VerticalMotionEnvelopeCheckTest {
  @Test
  void flagsSustainedAirborneVerticalEnvelopeViolations() {
    VerticalMotionEnvelopeCheck check = new VerticalMotionEnvelopeCheck(2);

    check.evaluate(
        new MovementEvent(
            "player", System.nanoTime(), 0.01D, 0.80D, false, 0.0F, false, false, 50_000_000L));
    assertTrue(
        check.evaluate(
                new MovementEvent(
                    "player", System.nanoTime(), 0.01D, 0.75D, false, 0.0F, false, false, 50_000_000L))
            .suspicious());
  }

  @Test
  void ignoresGroundOrExemptStates() {
    VerticalMotionEnvelopeCheck check = new VerticalMotionEnvelopeCheck(2);

    assertFalse(
        check.evaluate(
                new MovementEvent(
                    "player", System.nanoTime(), 0.01D, 0.80D, true, 0.0F, false, false, 50_000_000L))
            .suspicious());

    assertFalse(
        check.evaluate(
                new MovementEvent(
                    "player", System.nanoTime(), 0.01D, 0.90D, false, 0.0F, true, false, 50_000_000L))
            .suspicious());
  }
}
