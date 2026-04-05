package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.AirStrafeAccelerationCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class AirStrafeAccelerationCheckTest {

  /** Constructs a MovementEvent for an airborne player at the given XZ speed (bps × 0.05s). */
  private static MovementEvent airborne(String playerId, double deltaXZ) {
    // interval 50ms → seconds=0.05 → speedBps = deltaXZ / 0.05
    return new MovementEvent(playerId, System.nanoTime(), deltaXZ, 0.02D, false, 0.0F, false, false, 50_000_000L);
  }

  @Test
  void flagsSpikesAboveEstablishedBaseline() {
    AirStrafeAccelerationCheck check = new AirStrafeAccelerationCheck(2);

    // Build a baseline window: moderate airborne speed (~5 bps)
    check.evaluate(airborne("p", 0.25D));
    check.evaluate(airborne("p", 0.25D));
    check.evaluate(airborne("p", 0.25D));

    // Spike to ~20 bps — well above 12.0 threshold and 6.0 delta above baseline.
    // First spike increments buffer; second triggers overLimit.
    check.evaluate(airborne("p", 1.0D));
    assertTrue(check.evaluate(airborne("p", 1.0D)).suspicious());
  }

  @Test
  void noFlagWhenBelowSpeedThreshold() {
    AirStrafeAccelerationCheck check = new AirStrafeAccelerationCheck(1);

    // Build baseline window at ~5 bps, then "spike" to only ~10 bps
    // (below MIN_SUSPICIOUS_SPEED_BPS=12.0 → no flag)
    check.evaluate(airborne("p", 0.25D));
    check.evaluate(airborne("p", 0.25D));
    check.evaluate(airborne("p", 0.25D));
    assertFalse(check.evaluate(airborne("p", 0.50D)).suspicious());
  }

  @Test
  void resetsWindowOnLanding() {
    AirStrafeAccelerationCheck check = new AirStrafeAccelerationCheck(1);

    // Build a baseline window airborne
    check.evaluate(airborne("p", 0.25D));
    check.evaluate(airborne("p", 0.25D));
    check.evaluate(airborne("p", 0.25D));

    // Land — window should be cleared
    check.evaluate(new MovementEvent("p", System.nanoTime(), 0.1D, 0.0D, true, 0.0F, false, false, 50_000_000L));

    // Next airborne event should have no baseline (EMPTY peek) → no spike flagged
    assertFalse(check.evaluate(airborne("p", 1.0D)).suspicious());
  }

  @Test
  void ignoresGroundedOrExemptMovement() {
    AirStrafeAccelerationCheck check = new AirStrafeAccelerationCheck(2);

    assertFalse(
        check.evaluate(
                new MovementEvent("player", System.nanoTime(), 1.00D, 0.0D, true, 0.0F, false, false, 50_000_000L))
            .suspicious());
    assertFalse(
        check.evaluate(
                new MovementEvent("player", System.nanoTime(), 1.00D, 0.0D, false, 0.0F, true, false, 50_000_000L))
            .suspicious());
  }
}
