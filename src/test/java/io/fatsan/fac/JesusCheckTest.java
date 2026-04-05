package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.JesusCheck;
import io.fatsan.fac.model.PlayerStateEvent;
import org.junit.jupiter.api.Test;

final class JesusCheckTest {

  private static PlayerStateEvent state(
      String id, double speedBps, double deltaY, boolean inWater, boolean inLava) {
    double seconds = 0.05D;
    double deltaXZ = speedBps * seconds;
    long interval = (long) (seconds * 1_000_000_000L);
    return new PlayerStateEvent(
        id, System.nanoTime(), deltaXZ, deltaY, false,
        true, false, false, false,
        inWater, inLava, false, false, false,
        0.0D, 0.0D, 0.0D, interval);
  }

  @Test
  void flagsHighSpeedFlatInWater() {
    JesusCheck check = new JesusCheck(3);

    // 7.0 bps with near-zero deltaY inside water
    for (int i = 0; i < 12; i++) {
      check.evaluate(state("p", 7.0D, 0.0D, true, false));
    }
    assertTrue(check.evaluate(state("p", 7.0D, 0.0D, true, false)).suspicious());
  }

  @Test
  void noFlagWhenSinking() {
    JesusCheck check = new JesusCheck(2);

    // High speed but clearly sinking (deltaY = -0.2) — legitimate swimming
    for (int i = 0; i < 15; i++) {
      check.evaluate(state("p", 6.0D, -0.2D, true, false));
    }
    assertFalse(check.evaluate(state("p", 6.0D, -0.2D, true, false)).suspicious());
  }

  @Test
  void noFlagOutsideFluid() {
    JesusCheck check = new JesusCheck(1);

    for (int i = 0; i < 15; i++) {
      check.evaluate(state("p", 7.0D, 0.0D, false, false));
    }
    assertFalse(check.evaluate(state("p", 7.0D, 0.0D, false, false)).suspicious());
  }

  @Test
  void flagsHighSpeedFlatInLava() {
    JesusCheck check = new JesusCheck(3);

    for (int i = 0; i < 12; i++) {
      check.evaluate(state("p", 6.5D, 0.01D, false, true));
    }
    assertTrue(check.evaluate(state("p", 6.5D, 0.01D, false, true)).suspicious());
  }
}
