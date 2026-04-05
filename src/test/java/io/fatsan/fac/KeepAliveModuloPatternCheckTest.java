package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.KeepAliveModuloPatternCheck;
import io.fatsan.fac.model.KeepAliveSignal;
import org.junit.jupiter.api.Test;

final class KeepAliveModuloPatternCheckTest {
  @Test
  void flagsModuloLockedPingPattern() {
    KeepAliveModuloPatternCheck check = new KeepAliveModuloPatternCheck(1);
    for (int i = 0; i < 12; i++) {
      check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 75L));
    }
    assertTrue(check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 80L)).suspicious());
  }
}
