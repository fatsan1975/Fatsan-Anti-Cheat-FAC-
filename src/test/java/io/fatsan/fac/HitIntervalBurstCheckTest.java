package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.HitIntervalBurstCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

final class HitIntervalBurstCheckTest {
  @Test
  void flagsUltraFastHitBurst() {
    HitIntervalBurstCheck check = new HitIntervalBurstCheck(2);
    for (int i = 0; i < 5; i++) {
      check.evaluate(new CombatHitEvent("p", System.nanoTime(), 2.9D, false, false, 0.0F, false, false, 30_000_000L, ""));
    }
    assertTrue(check.evaluate(new CombatHitEvent("p", System.nanoTime(), 2.8D, false, false, 0.0F, false, false, 30_000_000L, "")).suspicious());
  }
}
