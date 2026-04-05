package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.ReachHeuristicCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

class ReachHeuristicCheckTest {
  @Test
  void shouldFlagConsistentHighReach() {
    ReachHeuristicCheck check = new ReachHeuristicCheck(2);
    check.evaluate(new CombatHitEvent("player", System.nanoTime(), 3.6D, false, false, 0.0F, false, false, 70_000_000L, ""));
    assertTrue(check.evaluate(new CombatHitEvent("player", System.nanoTime(), 3.7D, false, false, 0.0F, false, false, 70_000_000L, "")).suspicious());
  }
}
