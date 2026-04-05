package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.BlockPlaceIntervalConsistencyCheck;
import io.fatsan.fac.model.BlockPlaceEventSignal;
import org.junit.jupiter.api.Test;

final class BlockPlaceIntervalConsistencyCheckTest {
  @Test
  void flagsStableRapidPlaceCadenceWhileMoving() {
    BlockPlaceIntervalConsistencyCheck check = new BlockPlaceIntervalConsistencyCheck(2);
    check.evaluate(new BlockPlaceEventSignal("p", System.nanoTime(), 70_000_000L, true, 0.28D, "stone"));
    check.evaluate(new BlockPlaceEventSignal("p", System.nanoTime(), 71_000_000L, true, 0.29D, "stone"));
    assertTrue(check.evaluate(new BlockPlaceEventSignal("p", System.nanoTime(), 70_000_000L, true, 0.30D, "stone")).suspicious());
  }
}
