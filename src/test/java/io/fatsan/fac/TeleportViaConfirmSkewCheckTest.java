package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.TeleportViaConfirmSkewCheck;
import io.fatsan.fac.model.TeleportSignal;
import org.junit.jupiter.api.Test;

class TeleportViaConfirmSkewCheckTest {
  @Test
  void shouldFlagOnFastTeleportConfirmSequence() {
    TeleportViaConfirmSkewCheck check = new TeleportViaConfirmSkewCheck(2);
    check.evaluate(new TeleportSignal("p", 100L));
    check.evaluate(new TeleportSignal("p", 150_000_000L));
    check.evaluate(new TeleportSignal("p", 210_000_000L));
    assertTrue(check.evaluate(new TeleportSignal("p", 260_000_000L)).suspicious());
  }
}
