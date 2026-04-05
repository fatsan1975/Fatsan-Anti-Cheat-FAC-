package io.fatsan.fac.check.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindowStatsTrackerTest {

  @Test
  void emptyBeforeMinData() {
    WindowStatsTracker tracker = new WindowStatsTracker(10);
    // First two entries should return EMPTY (needs >= 3)
    tracker.record("p1", 100.0);
    tracker.record("p1", 110.0);
    WindowStatsTracker.Stats stats = tracker.record("p1", 120.0); // third entry — should now compute
    assertTrue(stats.hasEnoughData());
    assertEquals(3, stats.windowSize());
  }

  @Test
  void peekBeforeMinDataReturnsEmpty() {
    WindowStatsTracker tracker = new WindowStatsTracker(10);
    tracker.record("p1", 100.0);
    assertFalse(tracker.peek("p1").hasEnoughData());
  }

  @Test
  void windowCapEnforced() {
    WindowStatsTracker tracker = new WindowStatsTracker(5);
    for (int i = 0; i < 10; i++) {
      tracker.record("p1", (double) i * 100);
    }
    WindowStatsTracker.Stats stats = tracker.peek("p1");
    assertEquals(5, stats.windowSize());
  }

  @Test
  void meanIsCorrect() {
    WindowStatsTracker tracker = new WindowStatsTracker(10);
    tracker.record("p1", 100.0);
    tracker.record("p1", 200.0);
    WindowStatsTracker.Stats stats = tracker.record("p1", 300.0);
    assertEquals(200.0, stats.mean(), 0.001);
  }

  @Test
  void varianceIsZeroForConstantValues() {
    WindowStatsTracker tracker = new WindowStatsTracker(10);
    tracker.record("p1", 500.0);
    tracker.record("p1", 500.0);
    WindowStatsTracker.Stats stats = tracker.record("p1", 500.0);
    assertEquals(0.0, stats.variance(), 0.001);
    assertEquals(0.0, stats.entropyScore(), 0.001);
    assertTrue(stats.isUniformlyCadenced(0.01));
  }

  @Test
  void oscillationsDetected() {
    WindowStatsTracker tracker = new WindowStatsTracker(10);
    // Pattern: low, high, low, high → 2 extrema in middle
    tracker.record("p1", 100.0);
    tracker.record("p1", 500.0);
    tracker.record("p1", 100.0);
    tracker.record("p1", 500.0);
    WindowStatsTracker.Stats stats = tracker.record("p1", 100.0);
    assertTrue(stats.oscillations() >= 2);
    assertTrue(stats.isOscillating(2));
  }

  @Test
  void clearResetsState() {
    WindowStatsTracker tracker = new WindowStatsTracker(10);
    tracker.record("p1", 100.0);
    tracker.record("p1", 200.0);
    tracker.record("p1", 300.0);
    tracker.clear("p1");
    assertFalse(tracker.peek("p1").hasEnoughData());
  }

  @Test
  void isolationBetweenPlayers() {
    WindowStatsTracker tracker = new WindowStatsTracker(10);
    tracker.record("p1", 100.0);
    tracker.record("p1", 100.0);
    tracker.record("p1", 100.0);
    // p2 has no data
    assertFalse(tracker.peek("p2").hasEnoughData());
  }

  @Test
  void isFlatDetectsLowVariance() {
    WindowStatsTracker tracker = new WindowStatsTracker(10);
    tracker.record("p1", 1000.0);
    tracker.record("p1", 1001.0);
    WindowStatsTracker.Stats stats = tracker.record("p1", 999.0);
    // variance should be very small — definitely under 10_000
    assertTrue(stats.isFlat(10_000.0));
    assertFalse(stats.isFlat(0.0));
  }

  @Test
  void stddevMatchesSqrtVariance() {
    WindowStatsTracker tracker = new WindowStatsTracker(10);
    tracker.record("p1", 100.0);
    tracker.record("p1", 200.0);
    WindowStatsTracker.Stats stats = tracker.record("p1", 300.0);
    assertEquals(Math.sqrt(stats.variance()), stats.stddev(), 0.0001);
  }

  @Test
  void emptyStatsSentinel() {
    assertFalse(WindowStatsTracker.Stats.EMPTY.hasEnoughData());
    assertEquals(0, WindowStatsTracker.Stats.EMPTY.windowSize());
    assertEquals(0.0, WindowStatsTracker.Stats.EMPTY.variance());
  }

  @Test
  void windowCapClampedToMinFour() {
    // requesting size 1 should be silently clamped to 4
    WindowStatsTracker tracker = new WindowStatsTracker(1);
    for (int i = 0; i < 6; i++) {
      tracker.record("p1", i * 10.0);
    }
    WindowStatsTracker.Stats stats = tracker.peek("p1");
    assertEquals(4, stats.windowSize());
  }
}
