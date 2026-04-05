package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.KeepAliveConsistencyCheck;
import io.fatsan.fac.model.KeepAliveSignal;
import org.junit.jupiter.api.Test;

class KeepAliveConsistencyCheckTest {
  @Test
  void shouldFlagFrozenHighPingPattern() {
    KeepAliveConsistencyCheck check = new KeepAliveConsistencyCheck(2);
    check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 700));
    check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 700));
    assertTrue(check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 701)).suspicious());
  }
}
