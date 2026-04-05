package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.MovementCadenceCheck;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

class MovementCadenceCheckTest {
  @Test
  void shouldEscalateAfterBufferLimit() {
    MovementCadenceCheck check = new MovementCadenceCheck(3);
    CheckResult result = CheckResult.clean("init", CheckCategory.MOVEMENT);
    for (int i = 0; i < 3; i++) {
      result = check.evaluate(new MovementEvent("playerA", System.nanoTime(), 1.8D, 0.0D, true, 0.0F, false, false, 50_000_000L));
    }
    assertTrue(result.suspicious());
  }

  @Test
  void shouldCoolDownOnNormalInput() {
    MovementCadenceCheck check = new MovementCadenceCheck(2);
    check.evaluate(new MovementEvent("playerA", System.nanoTime(), 1.5D, 0.0D, true, 0.0F, false, false, 50_000_000L));
    CheckResult result = check.evaluate(new MovementEvent("playerA", System.nanoTime(), 0.1D, 0.0D, true, 0.0F, false, false, 50_000_000L));
    assertFalse(result.suspicious());
  }
}
