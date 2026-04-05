package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.ImpossibleCriticalCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

final class ImpossibleCriticalCheckTest {
  @Test
  void flagsCriticalLikeHitsInImpossibleState() {
    ImpossibleCriticalCheck check = new ImpossibleCriticalCheck(2);

    check.evaluate(
        new CombatHitEvent(
            "player", System.nanoTime(), 2.9D, true, true, 0.0F, false, false, 80_000_000L, ""));
    assertTrue(
        check.evaluate(
                new CombatHitEvent(
                    "player", System.nanoTime(), 2.8D, true, true, 0.0F, false, false, 80_000_000L, ""))
            .suspicious());
  }

  @Test
  void ignoresLegitAirCriticalLikeHits() {
    ImpossibleCriticalCheck check = new ImpossibleCriticalCheck(2);

    assertFalse(
        check.evaluate(
                new CombatHitEvent(
                    "player", System.nanoTime(), 2.9D, true, false, 0.35F, false, false, 80_000_000L, ""))
            .suspicious());
  }
}
