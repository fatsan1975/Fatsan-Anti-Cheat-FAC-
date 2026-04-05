package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.CombatProtocolTimingMismatchCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

final class CombatProtocolTimingMismatchCheckTest {
  @Test
  void flagsCombatTimingMismatch() {
    CombatProtocolTimingMismatchCheck check = new CombatProtocolTimingMismatchCheck(1);
    for (int i = 0; i < 8; i++) {
      check.evaluate(new CombatHitEvent("p", System.nanoTime(), 3.2D, false, true, 0.0F, false, false, 30_000_000L, ""));
    }
    assertTrue(check.evaluate(new CombatHitEvent("p", System.nanoTime(), 3.3D, false, true, 0.0F, false, false, 29_000_000L, "")).suspicious());
  }
}
