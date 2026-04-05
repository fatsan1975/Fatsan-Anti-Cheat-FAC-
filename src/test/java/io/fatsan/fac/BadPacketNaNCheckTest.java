package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.BadPacketNaNCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

class BadPacketNaNCheckTest {
  @Test
  void shouldFlagNonFinite() {
    BadPacketNaNCheck check = new BadPacketNaNCheck();
    assertTrue(check.evaluate(new MovementEvent("p", System.nanoTime(), Double.NaN, 0.0D, true, 0.0F, false, false, 50_000_000L)).suspicious());
  }

  @Test
  void shouldNotFlagFinite() {
    BadPacketNaNCheck check = new BadPacketNaNCheck();
    assertFalse(check.evaluate(new MovementEvent("p", System.nanoTime(), 0.2D, 0.0D, true, 0.0F, false, false, 50_000_000L)).suspicious());
  }
}
