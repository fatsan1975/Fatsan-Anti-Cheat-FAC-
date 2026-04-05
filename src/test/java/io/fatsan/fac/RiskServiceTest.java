package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.service.RiskService;
import org.junit.jupiter.api.Test;

class RiskServiceTest {
  @Test
  void shouldIncreaseRiskWhenSuspicionArrives() {
    RiskService riskService = new RiskService();
    CheckResult result =
        new CheckResult(true, "MovementCadence", CheckCategory.MOVEMENT, "test", 0.8D, true);
    double risk1 = riskService.apply("player", result, 1.0D);
    double risk2 = riskService.apply("player", result, 1.0D);
    assertTrue(risk2 > risk1);
  }
}
