package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.ReachVarianceCollapseCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

final class ReachVarianceCollapseCheckTest {
  @Test
  void flagsCollapsedHighReachVarianceWindow() {
    ReachVarianceCollapseCheck check = new ReachVarianceCollapseCheck(2);
    for (int i = 0; i < 7; i++) {
      check.evaluate(new CombatHitEvent("p", System.nanoTime(), 3.18D + ((i % 2 == 0) ? 0.01D : -0.01D), false, false, 0.0F, false, false, 80_000_000L, ""));
    }
    assertTrue(check.evaluate(new CombatHitEvent("p", System.nanoTime(), 3.19D, false, false, 0.0F, false, false, 80_000_000L, "")).suspicious());
  }
}
