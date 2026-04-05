package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.PingOscillationSpoofCheck;
import io.fatsan.fac.model.KeepAliveSignal;
import org.junit.jupiter.api.Test;

final class PingOscillationSpoofCheckTest {
  @Test
  void flagsHighAmplitudeAlternatingPingPattern() {
    PingOscillationSpoofCheck check = new PingOscillationSpoofCheck(2);
    long now = System.nanoTime();

    check.evaluate(new KeepAliveSignal("player", now, 80));
    check.evaluate(new KeepAliveSignal("player", now + 1, 320));
    check.evaluate(new KeepAliveSignal("player", now + 2, 90));
    check.evaluate(new KeepAliveSignal("player", now + 3, 330));
    check.evaluate(new KeepAliveSignal("player", now + 4, 85));
    check.evaluate(new KeepAliveSignal("player", now + 5, 340));
    assertTrue(check.evaluate(new KeepAliveSignal("player", now + 6, 80)).suspicious());
  }

  @Test
  void ignoresStablePing() {
    PingOscillationSpoofCheck check = new PingOscillationSpoofCheck(2);
    long now = System.nanoTime();

    check.evaluate(new KeepAliveSignal("player", now, 90));
    assertFalse(check.evaluate(new KeepAliveSignal("player", now + 1, 95)).suspicious());
  }
}
