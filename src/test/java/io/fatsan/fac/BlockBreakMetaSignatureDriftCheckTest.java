package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.BlockBreakMetaSignatureDriftCheck;
import io.fatsan.fac.model.BlockBreakEventSignal;
import org.junit.jupiter.api.Test;

class BlockBreakMetaSignatureDriftCheckTest {
  @Test
  void shouldFlagForCustomItemMetaSpeedMismatch() {
    BlockBreakMetaSignatureDriftCheck check = new BlockBreakMetaSignatureDriftCheck(2);
    BlockBreakEventSignal e =
        new BlockBreakEventSignal("p", 1L, 79_000_000L, 0, -1, -1, 4.0D, 0.2D, "NETHERITE_AXE", 0.95D, 0.21D, 11, true);
    check.evaluate(e);
    assertTrue(check.evaluate(e).suspicious());
  }
}
