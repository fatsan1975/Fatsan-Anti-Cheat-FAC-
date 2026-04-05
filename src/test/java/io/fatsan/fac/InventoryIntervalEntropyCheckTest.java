package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.InventoryIntervalEntropyCheck;
import io.fatsan.fac.model.InventoryClickEventSignal;
import org.junit.jupiter.api.Test;

final class InventoryIntervalEntropyCheckTest {
  @Test
  void flagsLowEntropyInventoryIntervals() {
    InventoryIntervalEntropyCheck check = new InventoryIntervalEntropyCheck(2);
    for (int i = 0; i < 9; i++) {
      check.evaluate(new InventoryClickEventSignal("p", System.nanoTime(), (i % 2 == 0) ? 80_000_000L : 81_000_000L, true, false));
    }
    assertTrue(check.evaluate(new InventoryClickEventSignal("p", System.nanoTime(), 80_000_000L, true, false)).suspicious());
  }
}
