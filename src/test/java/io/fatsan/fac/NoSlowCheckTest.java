package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.NoSlowCheck;
import io.fatsan.fac.model.PlayerStateEvent;
import org.junit.jupiter.api.Test;

final class NoSlowCheckTest {

  private static PlayerStateEvent state(
      String id, double speedBps, boolean eating, boolean blocking) {
    double seconds = 0.05D;
    double deltaXZ = speedBps * seconds;
    long interval = (long) (seconds * 1_000_000_000L);
    return new PlayerStateEvent(
        id, System.nanoTime(), deltaXZ, 0.0D, true,
        true, false, eating, blocking,
        false, false, false, false, false,
        0.0D, 0.0D, 0.0D, interval);
  }

  @Test
  void flagsSustainedFullSpeedWhileEating() {
    NoSlowCheck check = new NoSlowCheck(3);

    // Feed the window with eating + speed above 5.5 bps (limit 3 means buf triggers at 4+)
    for (int i = 0; i < 12; i++) {
      check.evaluate(state("p", 8.0D, true, false));
    }
    assertTrue(check.evaluate(state("p", 8.0D, true, false)).suspicious());
  }

  @Test
  void noFlagWhenSpeedIsLow() {
    NoSlowCheck check = new NoSlowCheck(2);

    // 3.0 bps while eating — below threshold
    for (int i = 0; i < 15; i++) {
      check.evaluate(state("p", 3.0D, true, false));
    }
    assertFalse(check.evaluate(state("p", 3.0D, true, false)).suspicious());
  }

  @Test
  void noFlagWhenNotUsingItem() {
    NoSlowCheck check = new NoSlowCheck(1);

    // Fast movement but not eating/blocking
    for (int i = 0; i < 15; i++) {
      check.evaluate(state("p", 10.0D, false, false));
    }
    assertFalse(check.evaluate(state("p", 10.0D, false, false)).suspicious());
  }

  @Test
  void flagsFullSpeedWhileBlocking() {
    NoSlowCheck check = new NoSlowCheck(3);

    for (int i = 0; i < 12; i++) {
      check.evaluate(state("p", 7.0D, false, true));
    }
    assertTrue(check.evaluate(state("p", 7.0D, false, true)).suspicious());
  }
}
