package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.HitReachSwitchPatternCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

final class HitReachSwitchPatternCheckTest {
  @Test
  void flagsAbruptReachSwitch() {
    HitReachSwitchPatternCheck check = new HitReachSwitchPatternCheck(1);
    check.evaluate(new CombatHitEvent("p", System.nanoTime(), 2.4D, false, false, 0.0F, false, false, 80_000_000L, ""));
    check.evaluate(new CombatHitEvent("p", System.nanoTime(), 3.5D, false, false, 0.0F, false, false, 80_000_000L, ""));
    check.evaluate(new CombatHitEvent("p", System.nanoTime(), 2.5D, false, false, 0.0F, false, false, 80_000_000L, ""));
    assertTrue(check.evaluate(new CombatHitEvent("p", System.nanoTime(), 3.45D, false, false, 0.0F, false, false, 80_000_000L, "")).suspicious());
  }
}
