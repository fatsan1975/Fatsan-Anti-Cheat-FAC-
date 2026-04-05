package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.VelocityManipulationCheck;
import io.fatsan.fac.model.PlayerStateEvent;
import io.fatsan.fac.service.VelocityTracker;
import org.junit.jupiter.api.Test;

final class VelocityManipulationCheckTest {

  private static PlayerStateEvent state(String id, double vx, double vy, double vz) {
    return new PlayerStateEvent(
        id, System.nanoTime(), 0.3D, 0.0D, true,
        true, false, false, false,
        false, false, false, false, false,
        vx, vy, vz, 50_000_000L);
  }

  @Test
  void flagsExcessiveHorizontalVelocity() {
    VelocityTracker tracker = new VelocityTracker();
    VelocityManipulationCheck check = new VelocityManipulationCheck(2, tracker);

    // vx=3.5, vz=0 → horizontal=3.5 > MAX_HORIZONTAL_VELOCITY=3.0
    check.evaluate(state("p", 3.5D, 0.0D, 0.0D));
    check.evaluate(state("p", 3.5D, 0.0D, 0.0D));
    assertTrue(check.evaluate(state("p", 3.5D, 0.0D, 0.0D)).suspicious());
  }

  @Test
  void flagsExcessiveVerticalVelocity() {
    VelocityTracker tracker = new VelocityTracker();
    VelocityManipulationCheck check = new VelocityManipulationCheck(2, tracker);

    // vy=2.8 > MAX_UPWARD_VELOCITY=2.5, horizontal=1.0 > noise floor
    check.evaluate(state("p", 1.0D, 2.8D, 0.0D));
    check.evaluate(state("p", 1.0D, 2.8D, 0.0D));
    assertTrue(check.evaluate(state("p", 1.0D, 2.8D, 0.0D)).suspicious());
  }

  @Test
  void noFlagNormalVelocity() {
    VelocityTracker tracker = new VelocityTracker();
    VelocityManipulationCheck check = new VelocityManipulationCheck(2, tracker);

    // Normal sprint velocity ~0.28 bpt = 5.6 bps, well within limits
    for (int i = 0; i < 10; i++) {
      assertFalse(check.evaluate(state("p", 0.28D, 0.0D, 0.0D)).suspicious());
    }
  }

  @Test
  void noFlagBelowNoiseFloor() {
    VelocityTracker tracker = new VelocityTracker();
    VelocityManipulationCheck check = new VelocityManipulationCheck(1, tracker);

    // horizontal < 0.5 noise floor — skip check entirely
    assertFalse(check.evaluate(state("p", 0.2D, 5.0D, 0.2D)).suspicious());
  }
}
