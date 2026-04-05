package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.*;
import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckResult;
import org.junit.jupiter.api.Test;

class DiagonalScaffoldCheckTest {
  private static BlockPlaceEventSignal place(String pid, boolean sprinting, double hSpeed, long intervalNanos) {
    return new BlockPlaceEventSignal(pid, System.nanoTime(), intervalNanos, sprinting, hSpeed, "COBBLESTONE");
  }

  @Test void noFlagForNormalPlacement() {
    assertFalse(new DiagonalScaffoldCheck(6).evaluate(place("p1", false, 0.1, 200_000_000L)).suspicious());
  }

  @Test void noFlagForSingleFastPlace() {
    assertFalse(new DiagonalScaffoldCheck(6).evaluate(place("p1", true, 0.3, 60_000_000L)).suspicious());
  }

  @Test void flagsForSustainedDiagonalScaffold() {
    var check = new DiagonalScaffoldCheck(3);
    for (int i = 0; i < 2; i++) check.evaluate(place("p1", true, 0.35, 55_000_000L));
    assertTrue(check.evaluate(place("p1", true, 0.35, 55_000_000L)).suspicious());
  }
}
