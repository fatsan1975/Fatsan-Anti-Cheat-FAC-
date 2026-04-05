package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.PingStepPatternCheck;
import io.fatsan.fac.model.KeepAliveSignal;
import org.junit.jupiter.api.Test;

final class PingStepPatternCheckTest {
  @Test
  void flagsFixedStepPingJumps() {
    PingStepPatternCheck check = new PingStepPatternCheck(2);
    long t = System.nanoTime();
    check.evaluate(new KeepAliveSignal("p", t, 100));
    check.evaluate(new KeepAliveSignal("p", t + 1, 150));
    check.evaluate(new KeepAliveSignal("p", t + 2, 200));
    check.evaluate(new KeepAliveSignal("p", t + 3, 250));
    check.evaluate(new KeepAliveSignal("p", t + 4, 300));
    assertTrue(check.evaluate(new KeepAliveSignal("p", t + 5, 350)).suspicious());
  }
}
