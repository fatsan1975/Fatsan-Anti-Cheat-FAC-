package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.TrafficFloodRampCheck;
import io.fatsan.fac.model.TrafficSignal;
import org.junit.jupiter.api.Test;

final class TrafficFloodRampCheckTest {
  @Test
  void flagsTrafficFloodRamp() {
    TrafficFloodRampCheck check = new TrafficFloodRampCheck(2);
    check.evaluate(new TrafficSignal("p", System.nanoTime(), 1000, 2));
    check.evaluate(new TrafficSignal("p", System.nanoTime(), 1100, 3));
    assertTrue(check.evaluate(new TrafficSignal("p", System.nanoTime(), 1200, 3)).suspicious());
  }
}
