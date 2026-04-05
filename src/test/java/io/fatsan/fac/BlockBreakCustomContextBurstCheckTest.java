package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.BlockBreakCustomContextBurstCheck;
import io.fatsan.fac.model.BlockBreakEventSignal;
import org.junit.jupiter.api.Test;

final class BlockBreakCustomContextBurstCheckTest {
  @Test
  void flagsCustomItemBurst() {
    BlockBreakCustomContextBurstCheck check = new BlockBreakCustomContextBurstCheck(1);
    for (int i = 0; i < 8; i++) {
      check.evaluate(new BlockBreakEventSignal("p", System.nanoTime(), 40_000_000L, 1, -1, -1, 4.0D, 0.1D, "DIAMOND_AXE", 1.1D, 0.0D, 10, true));
    }
    assertTrue(check.evaluate(new BlockBreakEventSignal("p", System.nanoTime(), 39_000_000L, 1, -1, -1, 4.0D, 0.1D, "DIAMOND_AXE", 1.1D, 0.0D, 10, true)).suspicious());
  }
}
