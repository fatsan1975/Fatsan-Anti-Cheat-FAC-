package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.*;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.PlayerStateEvent;
import io.fatsan.fac.service.VelocityTracker;
import org.junit.jupiter.api.Test;

class VelocityHorizontalCheckTest {
  private static PlayerStateEvent state(String pid, double dxz, double dy) {
    return new PlayerStateEvent(pid, System.nanoTime(), dxz, dy,
        true, false, false, false, false, false, false, false, false, false,
        0.0, 0.0, 0.0, 50_000_000L);
  }

  @Test void noFlagWhenNoKB() {
    assertFalse(new VelocityHorizontalCheck(6, new VelocityTracker()).evaluate(state("p1", 0.5, 0.0)).suspicious());
  }

  @Test void noFlagWhenHorizontalAccepted() {
    var vt = new VelocityTracker();
    vt.expectKnockback("p1", 0.4, 0.4);
    assertFalse(new VelocityHorizontalCheck(6, vt).evaluate(state("p1", 0.3, 0.4)).suspicious());
  }

  @Test void flagsWhenHorizontalCancelledVerticalAccepted() {
    var vt = new VelocityTracker();
    vt.expectKnockback("p1", 0.4, 0.4);
    var check = new VelocityHorizontalCheck(2, vt);
    check.evaluate(state("p1", 0.001, 0.4));
    vt.expectKnockback("p1", 0.4, 0.4);
    assertTrue(check.evaluate(state("p1", 0.001, 0.4)).suspicious());
  }
}
