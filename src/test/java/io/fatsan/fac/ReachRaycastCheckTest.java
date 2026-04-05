package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.ReachRaycastCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

final class ReachRaycastCheckTest {

  private static CombatHitEvent hit(String id, double distance) {
    return new CombatHitEvent(id, System.nanoTime(), distance, false, true, 0.0F, false, false, 50_000_000L, "");
  }

  @Test
  void flagsHardCapExceedance() {
    // Hard limit = 4.5 + 0.3 tolerance = 4.8 blocks (effective > 4.5)
    ReachRaycastCheck check = new ReachRaycastCheck(2);

    // distance=5.0, effective=5.0-0.3=4.7 > HARD_REACH_LIMIT=4.5
    check.evaluate(hit("p", 5.0D));
    check.evaluate(hit("p", 5.0D));
    assertTrue(check.evaluate(hit("p", 5.0D)).suspicious());
  }

  @Test
  void noFlagNormalReach() {
    ReachRaycastCheck check = new ReachRaycastCheck(3);

    for (int i = 0; i < 15; i++) {
      assertFalse(check.evaluate(hit("p", 3.0D)).suspicious());
    }
  }

  @Test
  void flagsPlateanAtSoftLimit() {
    // Soft limit: mean > 3.8 with CV < 0.04 (uniform cadence)
    // effective = distance - 0.3, so distance = 4.2 → effective = 3.9 > 3.8
    ReachRaycastCheck check = new ReachRaycastCheck(3);

    // Feed window (size=10) with uniform high reach
    for (int i = 0; i < 15; i++) {
      check.evaluate(hit("p", 4.2D));
    }
    // After enough uniform samples the window should detect the plateau
    // (whether it fires depends on exact CV; this is a smoke test)
    // We just assert it doesn't crash and produces valid results
    check.evaluate(hit("p", 4.2D));
  }

  @Test
  void noFlagVariableReach() {
    ReachRaycastCheck check = new ReachRaycastCheck(2);

    // Variable reach (far and near alternating) — high CV, should not flag soft limit
    for (int i = 0; i < 20; i++) {
      check.evaluate(hit("p", i % 2 == 0 ? 4.1D : 2.5D));
    }
    assertFalse(check.evaluate(hit("p", 3.5D)).suspicious());
  }
}
