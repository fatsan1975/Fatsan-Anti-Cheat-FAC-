package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.CombatHitDistancePlateauCheck;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.CheckResult;
import org.junit.jupiter.api.Test;

final class CombatHitDistancePlateauCheckTest {

  private static CombatHitEvent hit(String id, double distance) {
    return new CombatHitEvent(id, System.nanoTime(), distance, false, true, 0.0F, false, false, 100_000_000L, "");
  }

  @Test
  void flagsSustainedHighDistancePlateau() {
    CombatHitDistancePlateauCheck check = new CombatHitDistancePlateauCheck(2);

    // Uniform hits at 3.1 (> MIN_SUSPICIOUS_MEAN=2.9) with very low variance.
    // After 8 observations the CV will be ~0 (all same value) → isUniformlyCadenced(0.015) true.
    for (int i = 0; i < 7; i++) {
      check.evaluate(hit("p", 3.1D));
    }
    // 8th hit: window full → should increment buffer
    check.evaluate(hit("p", 3.1D));
    // 9th hit: second increment → overLimit → suspicious
    assertTrue(check.evaluate(hit("p", 3.1D)).suspicious());
  }

  @Test
  void noFlagWhenDistanceIsVanillaRange() {
    CombatHitDistancePlateauCheck check = new CombatHitDistancePlateauCheck(2);

    // Hits at 2.5 (< MIN_SUSPICIOUS_MEAN=2.9) — uniform but within vanilla range
    for (int i = 0; i < 10; i++) {
      assertFalse(check.evaluate(hit("p", 2.5D)).suspicious());
    }
  }

  @Test
  void noFlagWhenVarianceIsNatural() {
    CombatHitDistancePlateauCheck check = new CombatHitDistancePlateauCheck(1);

    // Alternating distances → high CV → not a plateau
    assertFalse(check.evaluate(hit("p", 2.9D)).suspicious());
    assertFalse(check.evaluate(hit("p", 3.5D)).suspicious());
    assertFalse(check.evaluate(hit("p", 2.9D)).suspicious());
    assertFalse(check.evaluate(hit("p", 3.5D)).suspicious());
    assertFalse(check.evaluate(hit("p", 2.9D)).suspicious());
    assertFalse(check.evaluate(hit("p", 3.5D)).suspicious());
    assertFalse(check.evaluate(hit("p", 2.9D)).suspicious());
    assertFalse(check.evaluate(hit("p", 3.5D)).suspicious());
  }
}
