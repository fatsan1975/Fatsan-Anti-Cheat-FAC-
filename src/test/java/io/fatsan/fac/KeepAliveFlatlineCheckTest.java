package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.KeepAliveFlatlineCheck;
import io.fatsan.fac.model.KeepAliveSignal;
import org.junit.jupiter.api.Test;

final class KeepAliveFlatlineCheckTest {
  @Test
  void flagsHighPingFlatlinePattern() {
    KeepAliveFlatlineCheck check = new KeepAliveFlatlineCheck(2);
    long t = System.nanoTime();
    for (int i = 0; i < 7; i++) {
      check.evaluate(new KeepAliveSignal("p", t + i, 300));
    }
    assertTrue(check.evaluate(new KeepAliveSignal("p", t + 8, 301)).suspicious());
  }
}
