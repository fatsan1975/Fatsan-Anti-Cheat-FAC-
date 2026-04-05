package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.TrafficDropRecoveryPatternCheck;
import io.fatsan.fac.model.TrafficSignal;
import org.junit.jupiter.api.Test;

final class TrafficDropRecoveryPatternCheckTest {
  @Test
  void flagsDropRecoveryOscillation() {
    TrafficDropRecoveryPatternCheck check = new TrafficDropRecoveryPatternCheck(1);
    check.evaluate(new TrafficSignal("p", System.nanoTime(), 920, 12));
    check.evaluate(new TrafficSignal("p", System.nanoTime(), 930, 0));
    check.evaluate(new TrafficSignal("p", System.nanoTime(), 940, 11));
    check.evaluate(new TrafficSignal("p", System.nanoTime(), 950, 0));
    assertTrue(check.evaluate(new TrafficSignal("p", System.nanoTime(), 960, 12)).suspicious());
  }
}
