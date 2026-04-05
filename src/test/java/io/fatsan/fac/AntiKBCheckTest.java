package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.AntiKBCheck;
import io.fatsan.fac.model.PlayerStateEvent;
import io.fatsan.fac.service.VelocityTracker;
import org.junit.jupiter.api.Test;

final class AntiKBCheckTest {

  // Creates a PlayerStateEvent with given horizontal speed (bps)
  private static PlayerStateEvent state(String id, double speedBps) {
    double seconds = 0.05D;
    double deltaXZ = speedBps * seconds;
    long interval = (long) (seconds * 1_000_000_000L);
    return new PlayerStateEvent(
        id, System.nanoTime(), deltaXZ, 0.0D, false,
        true, false, false, false,
        false, false, false, false, false,
        0.0D, 0.0D, 0.0D, interval);
  }

  @Test
  void flagsNearZeroKnockbackAbsorption() {
    VelocityTracker tracker = new VelocityTracker();
    AntiKBCheck check = new AntiKBCheck(2, tracker);

    // Expected knockback: 0.4 horizontal = 8.0 bps
    // Observed: 0.3 bps → ratio = 0.3/8.0 = 0.0375 < MIN_KB_RATIO=0.25 → suspicious
    tracker.expectKnockback("p", 0.4D, 0.4D);
    check.evaluate(state("p", 0.3D));
    tracker.expectKnockback("p", 0.4D, 0.4D);
    check.evaluate(state("p", 0.3D));
    tracker.expectKnockback("p", 0.4D, 0.4D);
    assertTrue(check.evaluate(state("p", 0.3D)).suspicious());
  }

  @Test
  void noFlagWhenKnockbackIsAbsorbed() {
    VelocityTracker tracker = new VelocityTracker();
    AntiKBCheck check = new AntiKBCheck(2, tracker);

    // Expected 0.4 horizontal = 8.0 bps, observed = 5.0 bps → ratio=0.625 > 0.25 → clean
    tracker.expectKnockback("p", 0.4D, 0.4D);
    assertFalse(check.evaluate(state("p", 5.0D)).suspicious());
  }

  @Test
  void noFlagWhenNoPendingKnockback() {
    VelocityTracker tracker = new VelocityTracker();
    AntiKBCheck check = new AntiKBCheck(1, tracker);

    // No expectKnockback call — consumeKnockback returns null
    assertFalse(check.evaluate(state("p", 0.0D)).suspicious());
  }

  @Test
  void noFlagWhenExpectedKBTooLow() {
    VelocityTracker tracker = new VelocityTracker();
    AntiKBCheck check = new AntiKBCheck(1, tracker);

    // expectedHorizontal=0.05 → 0.05*20=1.0 bps < MIN_EXPECTED_KB_BPS=2.0 → skip
    tracker.expectKnockback("p", 0.05D, 0.05D);
    assertFalse(check.evaluate(state("p", 0.0D)).suspicious());
  }
}
