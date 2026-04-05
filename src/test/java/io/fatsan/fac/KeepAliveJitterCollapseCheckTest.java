package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.KeepAliveJitterCollapseCheck;
import io.fatsan.fac.model.KeepAliveSignal;
import org.junit.jupiter.api.Test;

final class KeepAliveJitterCollapseCheckTest {
  @Test
  void flagsFlatJitterPattern() {
    KeepAliveJitterCollapseCheck check = new KeepAliveJitterCollapseCheck(1);
    for (int i = 0; i < 14; i++) {
      check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 62L));
    }
    assertTrue(check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 62L)).suspicious());
  }
}
