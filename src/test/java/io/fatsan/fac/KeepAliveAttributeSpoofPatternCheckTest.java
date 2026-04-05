package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.KeepAliveAttributeSpoofPatternCheck;
import io.fatsan.fac.model.KeepAliveSignal;
import org.junit.jupiter.api.Test;

final class KeepAliveAttributeSpoofPatternCheckTest {
  @Test
  void flagsKeepAliveSpoofPattern() {
    KeepAliveAttributeSpoofPatternCheck check = new KeepAliveAttributeSpoofPatternCheck(1);
    for (int i = 0; i < 8; i++) {
      check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 260L));
    }
    assertTrue(check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 270L)).suspicious());
  }
}
