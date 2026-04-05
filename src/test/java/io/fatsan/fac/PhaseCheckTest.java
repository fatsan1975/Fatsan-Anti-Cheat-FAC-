package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.PhaseCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class PhaseCheckTest {

  // interval = 50ms, speedBps * 0.05 = deltaXZ
  private static MovementEvent move(
      String id, boolean onGround, double speedBps, double deltaY) {
    double seconds = 0.05D;
    double deltaXZ = speedBps * seconds;
    long interval = (long) (seconds * 1_000_000_000L);
    return new MovementEvent(id, System.nanoTime(), deltaXZ, deltaY, onGround, 0.0F, false, false, interval);
  }

  @Test
  void flagsGroundPlusLargePositiveDeltaY() {
    PhaseCheck check = new PhaseCheck(2);

    // onGround=true, speed=5bps, deltaY=1.2 (above MAX_GROUND_DELTA_Y=0.8)
    check.evaluate(move("p", true, 5.0D, 1.2D));
    check.evaluate(move("p", true, 5.0D, 1.2D));
    assertTrue(check.evaluate(move("p", true, 5.0D, 1.2D)).suspicious());
  }

  @Test
  void noFlagWhenAirborne() {
    PhaseCheck check = new PhaseCheck(1);

    // Even with large deltaY, if not on ground it's just jumping
    assertFalse(check.evaluate(move("p", false, 5.0D, 1.2D)).suspicious());
  }

  @Test
  void noFlagWhenDeltaYIsNormal() {
    PhaseCheck check = new PhaseCheck(1);

    // deltaY=0.5 is within normal step-up range
    for (int i = 0; i < 10; i++) {
      assertFalse(check.evaluate(move("p", true, 5.0D, 0.5D)).suspicious());
    }
  }

  @Test
  void noFlagWhenSpeedIsTooLow() {
    PhaseCheck check = new PhaseCheck(1);

    // Speed below MIN_LATERAL_BPS=2.0 bps → not suspicious even with large deltaY
    for (int i = 0; i < 10; i++) {
      assertFalse(check.evaluate(move("p", true, 1.0D, 1.2D)).suspicious());
    }
  }
}
