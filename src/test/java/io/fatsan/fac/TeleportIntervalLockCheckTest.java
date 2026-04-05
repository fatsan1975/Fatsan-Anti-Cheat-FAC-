package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.TeleportIntervalLockCheck;
import io.fatsan.fac.model.TeleportSignal;
import org.junit.jupiter.api.Test;

final class TeleportIntervalLockCheckTest {
  @Test
  void flagsTeleportTimingLock() {
    TeleportIntervalLockCheck check = new TeleportIntervalLockCheck(1);
    long n = System.nanoTime();
    for (int i = 1; i <= 8; i++) {
      check.evaluate(new TeleportSignal("p", n + i * 500_000_000L));
    }
    assertTrue(check.evaluate(new TeleportSignal("p", n + 9L * 500_000_000L)).suspicious());
  }
}
