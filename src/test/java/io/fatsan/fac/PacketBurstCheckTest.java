package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.PacketBurstCheck;
import io.fatsan.fac.model.TrafficSignal;
import org.junit.jupiter.api.Test;

class PacketBurstCheckTest {
  @Test
  void shouldFlagBurstAfterBufferLimit() {
    PacketBurstCheck check = new PacketBurstCheck(2);
    check.evaluate(new TrafficSignal("player", System.nanoTime(), 600, 30));
    assertTrue(check.evaluate(new TrafficSignal("player", System.nanoTime(), 620, 35)).suspicious());
  }
}
