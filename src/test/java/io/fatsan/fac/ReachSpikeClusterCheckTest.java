package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.ReachSpikeClusterCheck;
import io.fatsan.fac.model.CombatHitEvent;
import org.junit.jupiter.api.Test;

final class ReachSpikeClusterCheckTest {
  @Test
  void flagsClusteredReachSpikesInShortWindow() {
    ReachSpikeClusterCheck check = new ReachSpikeClusterCheck(2);
    long base = System.nanoTime();

    check.evaluate(new CombatHitEvent("player", base, 3.30D, false, false, 0.0F, false, false, 80_000_000L, ""));
    check.evaluate(
        new CombatHitEvent("player", base + 200_000_000L, 3.40D, false, false, 0.0F, false, false, 80_000_000L, ""));
    check.evaluate(
        new CombatHitEvent("player", base + 350_000_000L, 3.45D, false, false, 0.0F, false, false, 80_000_000L, ""));
    assertTrue(
        check.evaluate(
                new CombatHitEvent(
                    "player", base + 500_000_000L, 3.42D, false, false, 0.0F, false, false, 80_000_000L, ""))
            .suspicious());
  }

  @Test
  void ignoresSparseOrLowReachHits() {
    ReachSpikeClusterCheck check = new ReachSpikeClusterCheck(2);
    long base = System.nanoTime();

    check.evaluate(new CombatHitEvent("player", base, 3.10D, false, false, 0.0F, false, false, 80_000_000L, ""));
    assertFalse(
        check.evaluate(
                new CombatHitEvent(
                    "player", base + 1_200_000_000L, 3.20D, false, false, 0.0F, false, false, 80_000_000L, ""))
            .suspicious());
  }
}
