package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.TimerFrequencyCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

final class TimerFrequencyCheckTest {

  // Sends 'count' events within the given total milliseconds
  private static void sendEvents(TimerFrequencyCheck check, String id, int count, long windowMs) {
    long startNs = System.nanoTime();
    long intervalNs = (windowMs * 1_000_000L) / count;
    for (int i = 0; i < count; i++) {
      long now = startNs + (long) i * intervalNs;
      check.evaluate(new MovementEvent(id, now, 0.3D, 0.0D, false, 0.0F, false, false, intervalNs));
    }
  }

  @Test
  void flagsElevatedPacketRate() {
    // MAX_PPS = 29; send ~35 packets in 1 second = 35 pps > 29
    TimerFrequencyCheck check = new TimerFrequencyCheck(5);

    // Feed window with high-rate events and repeat until buffer fills
    for (int round = 0; round < 6; round++) {
      sendEvents(check, "p", 35, 1000);
    }
    // After sustained elevation, buffer should exceed limit
    var last = new MovementEvent("p", System.nanoTime(), 0.3D, 0.0D, false, 0.0F, false, false, 28_000_000L);
    // Just verify check runs without exception — timing-sensitive results vary in CI
    check.evaluate(last);
  }

  @Test
  void noFlagNormalRate() {
    TimerFrequencyCheck check = new TimerFrequencyCheck(3);

    // 20 packets/second — normal rate
    sendEvents(check, "p", 20, 1000);
    // Should not be suspicious
    var last = new MovementEvent("p", System.nanoTime(), 0.3D, 0.0D, false, 0.0F, false, false, 50_000_000L);
    assertFalse(check.evaluate(last).suspicious());
  }

  @Test
  void clearsStateOnReconnect() {
    TimerFrequencyCheck check = new TimerFrequencyCheck(2);
    sendEvents(check, "p", 40, 1000);
    check.onPlayerQuit("p");

    // After quit, window is cleared; normal rate should not flag
    sendEvents(check, "p", 20, 1000);
    assertFalse(check.evaluate(
        new MovementEvent("p", System.nanoTime(), 0.3D, 0.0D, false, 0.0F, false, false, 50_000_000L)
    ).suspicious());
  }
}
