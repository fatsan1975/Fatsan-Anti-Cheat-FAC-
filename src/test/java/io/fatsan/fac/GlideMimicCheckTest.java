package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.GlideMimicCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class GlideMimicCheckTest {

  // Creates a gliding MovementEvent; vertBps = deltaY / seconds
  private static MovementEvent gliding(String id, double speedBps, double vertBps) {
    double seconds = 0.05D;
    double deltaXZ = speedBps * seconds;
    double deltaY = vertBps * seconds;
    long interval = (long) (seconds * 1_000_000_000L);
    return new MovementEvent(id, System.nanoTime(), deltaXZ, deltaY, false, 0.0F, true, false, interval);
  }

  private static MovementEvent notGliding(String id) {
    return new MovementEvent(id, System.nanoTime(), 0.3D, -0.05D, false, 0.0F, false, false, 50_000_000L);
  }

  @Test
  void flagsImpossibleAscentLowSpeed() {
    GlideMimicCheck check = new GlideMimicCheck(3);

    // vertBps=5.0 (above MAX_GLIDE_UPWARD_BPS=4.0) with speedBps=4.0 (below MIN=7.0)
    for (int i = 0; i < 10; i++) {
      check.evaluate(gliding("p", 4.0D, 5.0D));
    }
    assertTrue(check.evaluate(gliding("p", 4.0D, 5.0D)).suspicious());
  }

  @Test
  void noFlagWhenNotGliding() {
    GlideMimicCheck check = new GlideMimicCheck(1);

    for (int i = 0; i < 15; i++) {
      assertFalse(check.evaluate(notGliding("p")).suspicious());
    }
  }

  @Test
  void noFlagLegitimateRocketBoost() {
    GlideMimicCheck check = new GlideMimicCheck(2);

    // High horizontal speed (firework boost gives high speed): speed=15bps which covers ascent
    for (int i = 0; i < 15; i++) {
      check.evaluate(gliding("p", 15.0D, 5.0D));
    }
    // speedBps=15.0 >= MIN_GLIDE_SUSTAIN_SPEED_BPS=7.0 → not flagged
    assertFalse(check.evaluate(gliding("p", 15.0D, 5.0D)).suspicious());
  }

  @Test
  void noFlagNaturalGlideDescent() {
    GlideMimicCheck check = new GlideMimicCheck(1);

    // Descending glide: vertBps is negative
    for (int i = 0; i < 15; i++) {
      assertFalse(check.evaluate(gliding("p", 12.0D, -2.0D)).suspicious());
    }
  }
}
