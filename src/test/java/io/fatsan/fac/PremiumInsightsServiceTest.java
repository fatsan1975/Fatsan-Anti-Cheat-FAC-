package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.service.PremiumInsightsService;
import org.junit.jupiter.api.Test;

class PremiumInsightsServiceTest {
  @Test
  void shouldAggregateAlertsAndTopChecks() {
    PremiumInsightsService service = new PremiumInsightsService();
    service.record(new CheckResult(true, "CheckA", CheckCategory.COMBAT, "r", 2.0, true));
    service.record(new CheckResult(true, "CheckA", CheckCategory.COMBAT, "r", 4.0, true));
    service.record(new CheckResult(true, "CheckB", CheckCategory.MOVEMENT, "r", 3.0, true));

    PremiumInsightsService.PremiumSnapshot snapshot = service.snapshot();

    assertEquals(3, snapshot.totalAlerts());
    assertEquals(3.0, snapshot.averageSeverity(), 0.001);
    assertEquals("CheckA=2", snapshot.topChecks().getFirst());
  }
}
