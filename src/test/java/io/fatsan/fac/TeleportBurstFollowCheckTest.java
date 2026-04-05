package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.TeleportBurstFollowCheck;
import io.fatsan.fac.model.TeleportSignal;
import org.junit.jupiter.api.Test;

final class TeleportBurstFollowCheckTest {
  @Test
  void flagsRapidTeleportBurstChain() {
    TeleportBurstFollowCheck check = new TeleportBurstFollowCheck(1);

    long now = System.nanoTime();
    check.evaluate(new TeleportSignal("p", now));
    check.evaluate(new TeleportSignal("p", now + 200_000_000L));

    assertTrue(check.evaluate(new TeleportSignal("p", now + 350_000_000L)).suspicious());
  }
}
