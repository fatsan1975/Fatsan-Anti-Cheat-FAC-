package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.VerticalDirectionFlipCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class VerticalDirectionFlipCheckTest {
  @Test
  void flagsRapidHighAmplitudeVerticalFlips() {
    VerticalDirectionFlipCheck check = new VerticalDirectionFlipCheck(2);

    check.evaluate(
        new MovementEvent(
            "player", System.nanoTime(), 0.05D, 0.60D, false, 0.0F, false, false, 50_000_000L));
    check.evaluate(
        new MovementEvent(
            "player", System.nanoTime(), 0.05D, -0.65D, false, 0.0F, false, false, 50_000_000L));
    assertTrue(
        check.evaluate(
                new MovementEvent(
                    "player", System.nanoTime(), 0.05D, 0.70D, false, 0.0F, false, false, 50_000_000L))
            .suspicious());
  }

  @Test
  void ignoresGroundedTransitions() {
    VerticalDirectionFlipCheck check = new VerticalDirectionFlipCheck(2);

    assertFalse(
        check.evaluate(
                new MovementEvent(
                    "player", System.nanoTime(), 0.05D, -0.70D, true, 0.0F, false, false, 50_000_000L))
            .suspicious());
  }
}
