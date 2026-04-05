package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.AutoTotemCheck;
import io.fatsan.fac.model.InventoryClickEventSignal;
import org.junit.jupiter.api.Test;

final class AutoTotemCheckTest {

  private static InventoryClickEventSignal offhand(long intervalNs) {
    return new InventoryClickEventSignal("p", System.nanoTime(), intervalNs, false, true);
  }

  private static InventoryClickEventSignal normal(long intervalNs) {
    return new InventoryClickEventSignal("p", System.nanoTime(), intervalNs, false, false);
  }

  @Test
  void flagsRapidOffhandSwap() {
    AutoTotemCheck check = new AutoTotemCheck(3);
    // 50ms interval = clearly automated (threshold 180ms)
    for (int i = 0; i < 8; i++) check.evaluate(offhand(50_000_000L));
    assertTrue(check.evaluate(offhand(50_000_000L)).suspicious());
  }

  @Test
  void noFlagHumanSpeed() {
    AutoTotemCheck check = new AutoTotemCheck(2);
    for (int i = 0; i < 10; i++) assertFalse(check.evaluate(offhand(250_000_000L)).suspicious());
  }

  @Test
  void noFlagNonOffhandClicks() {
    AutoTotemCheck check = new AutoTotemCheck(1);
    for (int i = 0; i < 10; i++) assertFalse(check.evaluate(normal(50_000_000L)).suspicious());
  }
}
