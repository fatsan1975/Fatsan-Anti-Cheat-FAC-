package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.BlockBreakAttributeOutlierCheck;
import io.fatsan.fac.model.BlockBreakEventSignal;
import org.junit.jupiter.api.Test;

final class BlockBreakAttributeOutlierCheckTest {
  @Test
  void flagsBreaksAssistedByExtremeAttributeContext() {
    BlockBreakAttributeOutlierCheck check = new BlockBreakAttributeOutlierCheck(1);
    for (int i = 0; i < 8; i++) {
      check.evaluate(new BlockBreakEventSignal("p", System.nanoTime(), 60_000_000L, 2, -1, -1, 9.0D, 0.45D, "NETHERITE_AXE", 1.2D, 0.15D, 12, true));
    }
    assertTrue(check.evaluate(new BlockBreakEventSignal("p", System.nanoTime(), 59_000_000L, 2, -1, -1, 9.0D, 0.45D, "NETHERITE_AXE", 1.2D, 0.15D, 12, true)).suspicious());
  }
}
