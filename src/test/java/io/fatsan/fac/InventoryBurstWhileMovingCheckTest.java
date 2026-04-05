package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.InventoryBurstWhileMovingCheck;
import io.fatsan.fac.model.InventoryClickEventSignal;
import org.junit.jupiter.api.Test;

final class InventoryBurstWhileMovingCheckTest {
  @Test
  void flagsInventoryBurstWhileMovingFast() {
    InventoryBurstWhileMovingCheck check = new InventoryBurstWhileMovingCheck(2);
    for (int i = 0; i < 5; i++) {
      check.evaluate(new InventoryClickEventSignal("p", System.nanoTime(), 70_000_000L, true, false));
    }
    assertTrue(check.evaluate(new InventoryClickEventSignal("p", System.nanoTime(), 70_000_000L, true, false)).suspicious());
  }
}
