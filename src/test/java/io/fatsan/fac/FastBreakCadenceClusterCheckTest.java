package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.FastBreakCadenceClusterCheck;
import io.fatsan.fac.model.BlockBreakEventSignal;
import org.junit.jupiter.api.Test;

final class FastBreakCadenceClusterCheckTest {
  @Test
  void flagsClusteredFastBreakPattern() {
    FastBreakCadenceClusterCheck check = new FastBreakCadenceClusterCheck(2);
    long base = System.nanoTime();
    for (int i = 0; i < 6; i++) {
      check.evaluate(new BlockBreakEventSignal("p", base + i * 100_000_000L, 100_000_000L, 0, -1, -1, 4.0D, 0.1D, "DIAMOND_PICKAXE", 0.0D, 0.0D, 0, false));
    }
    assertTrue(check.evaluate(new BlockBreakEventSignal("p", base + 700_000_000L, 100_000_000L, 0, -1, -1, 4.0D, 0.1D, "DIAMOND_PICKAXE", 0.0D, 0.0D, 0, false)).suspicious());
  }
}
