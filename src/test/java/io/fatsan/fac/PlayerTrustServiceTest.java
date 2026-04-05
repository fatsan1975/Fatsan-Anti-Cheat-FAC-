package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.service.PlayerTrustService;
import org.junit.jupiter.api.Test;

class PlayerTrustServiceTest {
  @Test
  void shouldLowerTrustAndIncreaseRiskMultiplierAfterSuspicion() {
    PlayerTrustService trustService = new PlayerTrustService();
    String playerId = "player";
    double initialMultiplier = trustService.riskMultiplier(playerId);

    CheckResult result =
        new CheckResult(true, "RotationQuantization", CheckCategory.COMBAT, "test", 0.9D, true);
    trustService.onSuspicion(playerId, result);

    assertTrue(trustService.riskMultiplier(playerId) > initialMultiplier);
  }

  @Test
  void shouldRecoverTrustOnCleanEvents() {
    PlayerTrustService trustService = new PlayerTrustService();
    String playerId = "player";
    CheckResult result =
        new CheckResult(true, "PingSpoof", CheckCategory.PROTOCOL, "test", 0.8D, true);

    trustService.onSuspicion(playerId, result);
    double afterSuspicion = trustService.trustScore(playerId);
    for (int i = 0; i < 200; i++) {
      trustService.onCleanEvent(playerId);
    }

    assertTrue(trustService.trustScore(playerId) > afterSuspicion);
  }
}
