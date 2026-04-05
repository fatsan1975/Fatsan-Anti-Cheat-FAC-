package io.fatsan.fac;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.service.CorroborationService;
import io.fatsan.fac.service.PlayerTrustService;
import io.fatsan.fac.service.RiskService;
import io.fatsan.fac.service.SuspicionPatternService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that all per-player service state is correctly released when
 * clearPlayer is called on disconnect.
 *
 * <p>This prevents unbounded memory growth on servers with high player turnover
 * (hub networks, BungeeCord) where players connect and disconnect rapidly.
 */
class PlayerQuitCleanupTest {

  private static final String PLAYER = "11111111-1111-1111-1111-111111111111";

  private static CheckResult movement(double severity) {
    return new CheckResult(true, "SpeedEnvelope", CheckCategory.MOVEMENT,
        "test", severity, true);
  }

  // ── RiskService ─────────────────────────────────────────────────────────

  @Test
  void riskService_clearPlayerResetsRisk() {
    RiskService svc = new RiskService();
    svc.apply(PLAYER, movement(0.8), 1.0);
    assertTrue(svc.currentRisk(PLAYER) > 0.0, "Risk should accumulate before clear");

    svc.clearPlayer(PLAYER);

    assertEquals(0.0, svc.currentRisk(PLAYER), 0.001,
        "Risk should be 0 after clearPlayer");
  }

  @Test
  void riskService_clearSafeForUnknownPlayer() {
    RiskService svc = new RiskService();
    assertDoesNotThrow(() -> svc.clearPlayer("unknown-player-uuid"));
  }

  @Test
  void riskService_clearDoesNotAffectOtherPlayers() {
    RiskService svc = new RiskService();
    String other = "22222222-2222-2222-2222-222222222222";
    svc.apply(PLAYER, movement(0.8), 1.0);
    svc.apply(other, movement(0.8), 1.0);

    svc.clearPlayer(PLAYER);

    assertEquals(0.0, svc.currentRisk(PLAYER), 0.001);
    assertTrue(svc.currentRisk(other) > 0.0, "Other player risk should be unaffected");
  }

  // ── PlayerTrustService ──────────────────────────────────────────────────

  @Test
  void trustService_clearPlayerResetsToInitial() {
    PlayerTrustService svc = new PlayerTrustService();
    // Penalise trust heavily
    for (int i = 0; i < 20; i++) {
      svc.onSuspicion(PLAYER, movement(0.9));
    }
    double penalisedTrust = svc.trustScore(PLAYER);
    assertTrue(penalisedTrust < 0.55, "Trust should be below initial after penalties");

    svc.clearPlayer(PLAYER);

    // After clear, new trust should come back at initial (0.55) on next access
    assertEquals(0.55, svc.trustScore(PLAYER), 0.01,
        "Trust should reset to initial after clearPlayer");
  }

  @Test
  void trustService_clearSafeForUnknownPlayer() {
    PlayerTrustService svc = new PlayerTrustService();
    assertDoesNotThrow(() -> svc.clearPlayer("unknown-player-uuid"));
  }

  // ── SuspicionPatternService ─────────────────────────────────────────────

  @Test
  void suspicionPattern_clearPlayerResetsIntensity() {
    SuspicionPatternService svc = new SuspicionPatternService();
    svc.onSuspicion(PLAYER, movement(0.8));
    svc.onSuspicion(PLAYER, movement(0.8));
    assertTrue(svc.recentIntensity(PLAYER) > 0.0, "Intensity should accumulate");

    svc.clearPlayer(PLAYER);

    assertEquals(0.0, svc.recentIntensity(PLAYER), 0.001,
        "Intensity should be 0 after clearPlayer");
  }

  @Test
  void suspicionPattern_clearSafeForUnknownPlayer() {
    SuspicionPatternService svc = new SuspicionPatternService();
    assertDoesNotThrow(() -> svc.clearPlayer("unknown-player-uuid"));
  }

  // ── CorroborationService ────────────────────────────────────────────────

  @Test
  void corroboration_clearPlayerRemovesEvidence() {
    CorroborationService svc = new CorroborationService(5000L, 2, 4);
    CheckResult combat = new CheckResult(true, "ReachHeuristic", CheckCategory.COMBAT,
        "test", 0.8, true);
    CheckResult movement = movement(0.7);

    svc.record(PLAYER, combat);
    svc.record(PLAYER, combat);
    svc.record(PLAYER, movement);
    svc.record(PLAYER, movement);
    assertTrue(svc.isCorroborated(PLAYER), "Should be corroborated before clear");

    svc.clearPlayer(PLAYER);

    assertFalse(svc.isCorroborated(PLAYER), "Should not be corroborated after clear");
  }

  @Test
  void corroboration_clearSafeForUnknownPlayer() {
    CorroborationService svc = new CorroborationService(5000L, 2, 4);
    assertDoesNotThrow(() -> svc.clearPlayer("unknown-player-uuid"));
  }

  @Test
  void corroboration_clearIsolated() {
    CorroborationService svc = new CorroborationService(5000L, 2, 4);
    String other = "33333333-3333-3333-3333-333333333333";
    CheckResult combat = new CheckResult(true, "ReachHeuristic", CheckCategory.COMBAT,
        "test", 0.8, true);

    svc.record(PLAYER, combat);
    svc.record(PLAYER, combat);
    svc.record(other, combat);
    svc.record(other, combat);
    svc.record(other, movement(0.7));
    svc.record(other, movement(0.7));

    svc.clearPlayer(PLAYER);

    // other player should still be corroborated
    assertTrue(svc.isCorroborated(other), "Other player corroboration unaffected");
  }
}
