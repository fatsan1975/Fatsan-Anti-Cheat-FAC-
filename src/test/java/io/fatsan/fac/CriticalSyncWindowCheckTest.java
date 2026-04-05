package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.CriticalSyncWindowCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

final class CriticalSyncWindowCheckTest {
  @Test
  void flagsTightCriticalTimingStreak() {
    CriticalSyncWindowCheck check = new CriticalSyncWindowCheck(1);
    for (int i = 0; i < 8; i++) {
      check.evaluate(new CombatHitEvent("p", System.nanoTime(), 3.1D, true, false, 0.2F, false, false, 80_000_000L, ""));
    }
    assertTrue(check.evaluate(new CombatHitEvent("p", System.nanoTime(), 3.2D, true, false, 0.25F, false, false, 79_000_000L, "")).suspicious());
  }
}
