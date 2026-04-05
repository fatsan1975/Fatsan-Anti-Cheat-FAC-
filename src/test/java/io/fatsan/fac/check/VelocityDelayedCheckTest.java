package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.*;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.PlayerStateEvent;
import org.junit.jupiter.api.Test;

class VelocityDelayedCheckTest {
  private static PlayerStateEvent state(String pid, double dxz, double dy) {
    return new PlayerStateEvent(pid, System.nanoTime(), dxz, dy,
        true, false, false, false, false, false, false, false, false, false,
        0.0, 0.0, 0.0, 50_000_000L);
  }

  @Test void noFlagWhenNoKBHistory() {
    assertFalse(new VelocityDelayedCheck(6).evaluate(state("p1", 0.5, 0.3)).suspicious());
  }

  @Test void noFlagWhenKBAppliedWithinWindow() {
    var check = new VelocityDelayedCheck(6);
    check.recordKnockback("p1");
    check.recordKnockbackResponse("p1");
    assertFalse(check.evaluate(state("p1", 0.5, 0.3)).suspicious());
  }

  @Test void flagsWhenKBResponseDelayed() {
    var check = new VelocityDelayedCheck(1);
    check.recordKnockbackWithTimestamp("p1", System.nanoTime() - 1_000_000_000L);
    CheckResult r = check.evaluate(state("p1", 0.01, 0.0));
    assertTrue(r.suspicious());
  }
}
