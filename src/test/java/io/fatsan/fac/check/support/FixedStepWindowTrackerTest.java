package io.fatsan.fac.check.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FixedStepWindowTrackerTest {
  @Test
  void shouldBuildStreakOnlyForValuesInsideStepWindow() {
    FixedStepWindowTracker tracker = new FixedStepWindowTracker();

    assertEquals(0, tracker.record("p", 60L, 2L, 1L, 90L));
    assertEquals(1, tracker.record("p", 61L, 2L, 1L, 90L));
    assertEquals(2, tracker.record("p", 60L, 2L, 1L, 90L));
    assertEquals(0, tracker.record("p", 120L, 2L, 1L, 90L));
    assertEquals(1, tracker.record("p", 119L, 2L, 1L, 120L));
  }
}
