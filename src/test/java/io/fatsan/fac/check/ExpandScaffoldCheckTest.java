package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.*;
import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckResult;
import org.junit.jupiter.api.Test;

class ExpandScaffoldCheckTest {
  private static BlockPlaceEventSignal place(String pid, boolean sprinting, double hSpeed, long intervalNanos) {
    return new BlockPlaceEventSignal(pid, System.nanoTime(), intervalNanos, sprinting, hSpeed, "COBBLESTONE");
  }

  @Test void noFlagForNormalPlacement() {
    assertFalse(new ExpandScaffoldCheck(6).evaluate(place("p1", false, 0.1, 200_000_000L)).suspicious());
  }

  @Test void noFlagForSlowExpand() {
    assertFalse(new ExpandScaffoldCheck(6).evaluate(place("p1", true, 0.45, 300_000_000L)).suspicious());
  }

  @Test void flagsForFastExpand() {
    var check = new ExpandScaffoldCheck(3);
    for (int i = 0; i < 2; i++) check.evaluate(place("p1", true, 0.50, 50_000_000L));
    assertTrue(check.evaluate(place("p1", true, 0.50, 50_000_000L)).suspicious());
  }
}
