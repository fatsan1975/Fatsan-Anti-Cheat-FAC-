package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.KeepAlivePlateauDropCheck;
import io.fatsan.fac.model.KeepAliveSignal;
import org.junit.jupiter.api.Test;

final class KeepAlivePlateauDropCheckTest {
  @Test
  void flagsLargeInstantLatencyDropAfterHighPlateau() {
    KeepAlivePlateauDropCheck check = new KeepAlivePlateauDropCheck(1);

    check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 260L));
    check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 255L));

    assertTrue(check.evaluate(new KeepAliveSignal("p", System.nanoTime(), 110L)).suspicious());
  }
}
