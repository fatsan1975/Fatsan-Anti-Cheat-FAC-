package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.TeleportOrderHeuristicCheck;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.TeleportSignal;
import org.junit.jupiter.api.Test;

class TeleportOrderHeuristicCheckTest {
  @Test
  void shouldFlagImmediateDesyncPattern() {
    TeleportOrderHeuristicCheck check = new TeleportOrderHeuristicCheck(2);
    long t = System.nanoTime();
    check.evaluate(new TeleportSignal("p", t));
    check.evaluate(new MovementEvent("p", t + 2_000_000L, 4.0D, 0.0D, true, 0.0F, false, false, 2_000_000L));
    check.evaluate(new TeleportSignal("p", t + 10_000_000L));
    assertTrue(check.evaluate(new MovementEvent("p", t + 12_000_000L, 4.0D, 0.0D, true, 0.0F, false, false, 2_000_000L)).suspicious());
  }
}
