package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.AirHoverStreakCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class AirHoverStreakCheckTest {
  @Test
  void flagsSustainedAirHoverStreak() {
    AirHoverStreakCheck check = new AirHoverStreakCheck(2);
    for (int i = 0; i < 7; i++) {
      check.evaluate(new MovementEvent("p", System.nanoTime(), 0.15D, 0.001D, false, 0.0F, false, false, 50_000_000L));
    }
    assertTrue(check.evaluate(new MovementEvent("p", System.nanoTime(), 0.16D, 0.002D, false, 0.0F, false, false, 50_000_000L)).suspicious());
  }
}
