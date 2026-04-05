package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.InventoryProtocolDriftCheck;
import io.fatsan.fac.model.InventoryClickEventSignal;
import org.junit.jupiter.api.Test;

final class InventoryProtocolDriftCheckTest {
  @Test
  void flagsInventoryProtocolDrift() {
    InventoryProtocolDriftCheck check = new InventoryProtocolDriftCheck(1);
    for (int i = 0; i < 8; i++) {
      check.evaluate(new InventoryClickEventSignal("p", System.nanoTime(), 15_000_000L, false, false));
    }
    assertTrue(check.evaluate(new InventoryClickEventSignal("p", System.nanoTime(), 14_000_000L, false, false)).suspicious());
  }
}
