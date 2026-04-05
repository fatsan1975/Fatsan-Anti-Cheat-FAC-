package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.service.PlayerSignalTracker;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlayerSignalTrackerTest {
  @Test
  void samplesKeepAliveAtConfiguredInterval() {
    PlayerSignalTracker tracker = new PlayerSignalTracker();
    UUID id = UUID.randomUUID();

    assertTrue(tracker.shouldSampleKeepAlive(id, 1_000_000_000L, 200));
    assertFalse(tracker.shouldSampleKeepAlive(id, 1_100_000_000L, 200));
    assertTrue(tracker.shouldSampleKeepAlive(id, 1_250_000_000L, 200));
  }

  @Test
  void clearResetsKeepAliveSamplingState() {
    PlayerSignalTracker tracker = new PlayerSignalTracker();
    UUID id = UUID.randomUUID();

    assertTrue(tracker.shouldSampleKeepAlive(id, 1_000_000_000L, 250));
    tracker.clear(id);
    assertTrue(tracker.shouldSampleKeepAlive(id, 1_050_000_000L, 250));
  }
}
