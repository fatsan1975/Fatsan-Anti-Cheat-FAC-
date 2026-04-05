package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.CriticalCadenceAbuseCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

final class CriticalCadenceAbuseCheckTest {
  @Test
  void flagsSustainedRapidCriticalPattern() {
    CriticalCadenceAbuseCheck check = new CriticalCadenceAbuseCheck(2);

    check.evaluate(
        new CombatHitEvent(
            "player", System.nanoTime(), 2.8D, true, false, 0.14F, false, false, 85_000_000L, ""));
    check.evaluate(
        new CombatHitEvent(
            "player", System.nanoTime(), 2.8D, true, false, 0.16F, false, false, 80_000_000L, ""));
    check.evaluate(
        new CombatHitEvent(
            "player", System.nanoTime(), 2.9D, true, false, 0.13F, false, false, 90_000_000L, ""));
    assertTrue(
        check.evaluate(
                new CombatHitEvent(
                    "player", System.nanoTime(), 2.9D, true, false, 0.15F, false, false, 85_000_000L, ""))
            .suspicious());
  }

  @Test
  void ignoresLegitSlowHits() {
    CriticalCadenceAbuseCheck check = new CriticalCadenceAbuseCheck(2);

    assertFalse(
        check.evaluate(
                new CombatHitEvent(
                    "player", System.nanoTime(), 2.8D, true, false, 0.2F, false, false, 180_000_000L, ""))
            .suspicious());
  }
}
