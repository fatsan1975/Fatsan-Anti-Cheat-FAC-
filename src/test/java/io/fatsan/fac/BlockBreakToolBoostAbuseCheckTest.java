package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.BlockBreakToolBoostAbuseCheck;
import io.fatsan.fac.model.BlockBreakEventSignal;
import org.junit.jupiter.api.Test;

final class BlockBreakToolBoostAbuseCheckTest {
  @Test
  void flagsFastBreakWithoutBoostContext() {
    BlockBreakToolBoostAbuseCheck check = new BlockBreakToolBoostAbuseCheck(1);
    for (int i = 0; i < 8; i++) {
      check.evaluate(new BlockBreakEventSignal("p", System.nanoTime(), 50_000_000L, 0, -1, -1, 4.0D, 0.1D, "DIAMOND_PICKAXE", 0.0D, 0.0D, 0, false));
    }
    assertTrue(check.evaluate(new BlockBreakEventSignal("p", System.nanoTime(), 49_000_000L, 0, -1, -1, 4.0D, 0.1D, "DIAMOND_PICKAXE", 0.0D, 0.0D, 0, false)).suspicious());
  }
}
