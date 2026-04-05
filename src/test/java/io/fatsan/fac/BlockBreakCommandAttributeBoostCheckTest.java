package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.BlockBreakCommandAttributeBoostCheck;
import io.fatsan.fac.model.BlockBreakEventSignal;
import org.junit.jupiter.api.Test;

class BlockBreakCommandAttributeBoostCheckTest {
  @Test
  void shouldFlagWhenCommandLikeItemSpeedsUpBreaks() {
    BlockBreakCommandAttributeBoostCheck check = new BlockBreakCommandAttributeBoostCheck(2);
    BlockBreakEventSignal signal =
        new BlockBreakEventSignal("p", 1L, 50_000_000L, 1, -1, -1, 4.0D, 0.2D, "DIAMOND_PICKAXE", 0.2D, 0.25D, 6, true);
    check.evaluate(signal);
    assertTrue(check.evaluate(signal).suspicious());
  }
}
