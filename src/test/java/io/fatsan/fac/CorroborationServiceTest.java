package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.service.CorroborationService;
import org.junit.jupiter.api.Test;

class CorroborationServiceTest {
  @Test
  void shouldRequireDistinctCategoriesAndMinEvents() {
    CorroborationService service = new CorroborationService(10_000, 2, 3);
    service.record("p", new CheckResult(true, "MovementCadence", CheckCategory.MOVEMENT, "a", 0.7D, true));
    service.record("p", new CheckResult(true, "Timer", CheckCategory.MOVEMENT, "b", 0.7D, true));
    assertFalse(service.isCorroborated("p"));

    service.record("p", new CheckResult(true, "Reach", CheckCategory.COMBAT, "c", 0.9D, true));
    assertTrue(service.isCorroborated("p"));
  }
}
