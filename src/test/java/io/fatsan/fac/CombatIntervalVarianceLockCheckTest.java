package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.CombatIntervalVarianceLockCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

final class CombatIntervalVarianceLockCheckTest {
  @Test
  void flagsLockedCombatIntervals() {
    CombatIntervalVarianceLockCheck check = new CombatIntervalVarianceLockCheck(1);
    for (int i = 0; i < 10; i++) {
      check.evaluate(new CombatHitEvent("p", System.nanoTime(), 3.1D, false, true, 0.0F, false, false, 80_000_000L, ""));
    }
    assertTrue(check.evaluate(new CombatHitEvent("p", System.nanoTime(), 3.1D, false, true, 0.0F, false, false, 81_000_000L, "")).suspicious());
  }
}
