package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.BlockPlaceSprintCadenceCheck;
import io.fatsan.fac.model.BlockPlaceEventSignal;
import org.junit.jupiter.api.Test;

final class BlockPlaceSprintCadenceCheckTest {
  @Test
  void flagsSprintPlaceCadenceLock() {
    BlockPlaceSprintCadenceCheck check = new BlockPlaceSprintCadenceCheck(1);
    for (int i = 0; i < 10; i++) {
      check.evaluate(new BlockPlaceEventSignal("p", System.nanoTime(), 45_000_000L, true, 0.34D, "stone"));
    }
    assertTrue(check.evaluate(new BlockPlaceEventSignal("p", System.nanoTime(), 44_000_000L, true, 0.33D, "stone")).suspicious());
  }
}
