package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.TeleportChainCheck;
import io.fatsan.fac.model.TeleportSignal;
import org.junit.jupiter.api.Test;

final class TeleportChainCheckTest {
  @Test
  void flagsDenseTeleportChain() {
    TeleportChainCheck check = new TeleportChainCheck(2);
    long t = System.nanoTime();
    check.evaluate(new TeleportSignal("p", t));
    check.evaluate(new TeleportSignal("p", t + 200_000_000L));
    check.evaluate(new TeleportSignal("p", t + 400_000_000L));
    check.evaluate(new TeleportSignal("p", t + 600_000_000L));
    assertTrue(check.evaluate(new TeleportSignal("p", t + 800_000_000L)).suspicious());
  }
}
