package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.TrafficPacketEventsSkewCheck;
import io.fatsan.fac.model.TrafficSignal;
import org.junit.jupiter.api.Test;

final class TrafficPacketEventsSkewCheckTest {
  @Test
  void flagsPacketEventsSkew() {
    TrafficPacketEventsSkewCheck check = new TrafficPacketEventsSkewCheck(1);
    for (int i = 0; i < 8; i++) {
      check.evaluate(new TrafficSignal("p", System.nanoTime(), 1020, 3));
    }
    assertTrue(check.evaluate(new TrafficSignal("p", System.nanoTime(), 1100, 5)).suspicious());
  }
}
