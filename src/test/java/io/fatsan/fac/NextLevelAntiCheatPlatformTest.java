package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.nextlevel.NextLevelAntiCheatPlatform;
import org.junit.jupiter.api.Test;

class NextLevelAntiCheatPlatformTest {
  @Test
  void shouldProvideRoadmapPrimitives() {
    NextLevelAntiCheatPlatform platform = new NextLevelAntiCheatPlatform();

    assertEquals(2, platform.schemaVersion());
    assertTrue(platform.privacySafePlayerId("player-1").length() >= 10);
    assertTrue(platform.releaseGate(false, 0.03, 0.05));
  }

  @Test
  void shouldStoreFeedbackAndLabels() {
    NextLevelAntiCheatPlatform platform = new NextLevelAntiCheatPlatform();
    platform.pushGroundTruthLabel("p1", "review");
    platform.markFalsePositive("p2", "manual-check");

    NextLevelAntiCheatPlatform.NextLevelStatus status = platform.status();
    assertEquals(1, status.labels());
    assertEquals(1, status.falsePositiveFeedback());
  }
  @Test
  void shouldTrackCheckExecutionHealth() {
    NextLevelAntiCheatPlatform platform = new NextLevelAntiCheatPlatform();

    platform.onCheckExecution("SpeedEnvelope", new io.fatsan.fac.model.MovementEvent("p", 10L, 0.1, 0.0, true, 0F, false, false, 5L), 3_000_000L, false);
    platform.onCheckExecution("SpeedEnvelope", new io.fatsan.fac.model.MovementEvent("p", 12L, 0.2, 0.0, true, 0F, false, false, 5L), 2_000_000L, true);

    NextLevelAntiCheatPlatform.HealthSnapshot health = platform.selfHealth();
    assertTrue(health.worstCheckLatencyNanos() >= 3_000_000L);
    assertEquals(1L, health.totalCheckFailures());
  }


  @Test
  void shouldRejectSamplingForMissingPlayerIdAndTrackParseError() {
    NextLevelAntiCheatPlatform platform = new NextLevelAntiCheatPlatform();

    assertFalse(platform.shouldSample(null, false));
    assertEquals(1L, platform.dataQuality().parseErrors());
  }


  @Test
  void shouldAttributeFalsePositiveFeedbackToLastCheck() {
    NextLevelAntiCheatPlatform platform = new NextLevelAntiCheatPlatform();
    platform.onResult("p1", new io.fatsan.fac.model.CheckResult(true, "SpeedEnvelope", io.fatsan.fac.model.CheckCategory.MOVEMENT, "fast", 0.8D, true), 7.2D);
    platform.markFalsePositive("p1", "reviewed-clean");

    assertEquals(1L, platform.feedbackSummaryByCheck().get("SpeedEnvelope"));
  }

}
