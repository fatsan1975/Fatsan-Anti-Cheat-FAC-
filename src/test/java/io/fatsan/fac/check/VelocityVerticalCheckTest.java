package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.*;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.PlayerStateEvent;
import io.fatsan.fac.service.VelocityTracker;
import org.junit.jupiter.api.Test;

class VelocityVerticalCheckTest {
  private static PlayerStateEvent state(String pid, double dxz, double dy) {
    return new PlayerStateEvent(pid, System.nanoTime(), dxz, dy,
        true, false, false, false, false, false, false, false, false, false,
        0.0, 0.0, 0.0, 50_000_000L);
  }

  @Test void noFlagWhenNoKB() {
    assertFalse(new VelocityVerticalCheck(6, new VelocityTracker()).evaluate(state("p1", 0.5, 0.0)).suspicious());
  }

  @Test void noFlagWhenVerticalAccepted() {
    var vt = new VelocityTracker();
    vt.expectKnockback("p1", 0.4, 0.4);
    assertFalse(new VelocityVerticalCheck(6, vt).evaluate(state("p1", 0.3, 0.3)).suspicious());
  }

  @Test void flagsWhenVerticalCancelled() {
    var vt = new VelocityTracker();
    vt.expectKnockback("p1", 0.4, 0.4);
    var check = new VelocityVerticalCheck(2, vt);
    check.evaluate(state("p1", 0.3, 0.001));
    vt.expectKnockback("p1", 0.4, 0.4);
    assertTrue(check.evaluate(state("p1", 0.3, 0.001)).suspicious());
  }
}
