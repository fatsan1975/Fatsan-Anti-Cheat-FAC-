package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.KeepAliveRampAnomalyCheck;
import io.fatsan.fac.model.KeepAliveSignal;
import org.junit.jupiter.api.Test;

final class KeepAliveRampAnomalyCheckTest {
  @Test
  void flagsRapidPingRamp() {
    KeepAliveRampAnomalyCheck check = new KeepAliveRampAnomalyCheck(1);
    long ping = 90L;
    for (int i = 0; i < 7; i++) {
      check.evaluate(new KeepAliveSignal("p", System.nanoTime(), ping));
      ping += 30L;
    }
    assertTrue(check.evaluate(new KeepAliveSignal("p", System.nanoTime(), ping + 30L)).suspicious());
  }
}
