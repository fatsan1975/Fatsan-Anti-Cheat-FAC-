package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.KeepAliveDriftCheck;
import io.fatsan.fac.model.KeepAliveSignal;
import org.junit.jupiter.api.Test;

final class KeepAliveDriftCheckTest {
  @Test
  void flagsSustainedPingDriftEscalation() {
    KeepAliveDriftCheck check = new KeepAliveDriftCheck(2);
    long t = System.nanoTime();
    check.evaluate(new KeepAliveSignal("p", t, 50));
    check.evaluate(new KeepAliveSignal("p", t + 1, 130));
    check.evaluate(new KeepAliveSignal("p", t + 2, 210));
    check.evaluate(new KeepAliveSignal("p", t + 3, 290));
    check.evaluate(new KeepAliveSignal("p", t + 4, 370));
    assertTrue(check.evaluate(new KeepAliveSignal("p", t + 5, 450)).suspicious());
  }
}
