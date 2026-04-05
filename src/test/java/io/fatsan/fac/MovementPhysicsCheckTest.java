package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.MovementPhysicsCheck;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.service.MovementPhysicsValidator;
import org.junit.jupiter.api.Test;

final class MovementPhysicsCheckTest {

  // MAX_SPRINT_BPS = 6.0, tolerance TOLERANCE_FACTOR=1.25 → max predicted ≈ 7.5 bps
  // MAX_SPEED_RATIO = 1.6 → flags when mean ratio > 1.6

  private static MovementEvent move(String id, double speedBps) {
    double seconds = 0.05D;
    double deltaXZ = speedBps * seconds;
    long interval = (long) (seconds * 1_000_000_000L);
    return new MovementEvent(id, System.nanoTime(), deltaXZ, 0.0D, true, 0.0F, false, false, interval);
  }

  @Test
  void flagsSustainedOverspeed() {
    MovementPhysicsCheck check = new MovementPhysicsCheck(4, new MovementPhysicsValidator());

    // MAX_SPRINT_BPS * 1.25 * 1.6 = 6.0 * 1.25 * 1.6 = 12.0 bps threshold
    // Send 20 bps to clearly exceed ratio > 1.6
    for (int i = 0; i < 15; i++) {
      check.evaluate(move("p", 20.0D));
    }
    assertTrue(check.evaluate(move("p", 20.0D)).suspicious());
  }

  @Test
  void noFlagNormalSprintSpeed() {
    MovementPhysicsCheck check = new MovementPhysicsCheck(2, new MovementPhysicsValidator());

    // Normal sprint ~5.6 bps — well within limits
    for (int i = 0; i < 15; i++) {
      assertFalse(check.evaluate(move("p", 5.6D)).suspicious());
    }
  }

  @Test
  void skipWhenGliding() {
    MovementPhysicsCheck check = new MovementPhysicsCheck(1, new MovementPhysicsValidator());

    // Gliding events should not flag (exempt)
    for (int i = 0; i < 15; i++) {
      var glide = new MovementEvent("p", System.nanoTime(), 1.0D, -0.1D, false, 0.0F, true, false, 50_000_000L);
      assertFalse(check.evaluate(glide).suspicious());
    }
  }
}
