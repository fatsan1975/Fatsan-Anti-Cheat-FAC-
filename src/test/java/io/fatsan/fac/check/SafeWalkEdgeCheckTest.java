package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.*;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.PlayerStateEvent;
import org.junit.jupiter.api.Test;

class SafeWalkEdgeCheckTest {
  private static PlayerStateEvent state(String pid, boolean sneaking, boolean onGround, double dxz) {
    return new PlayerStateEvent(pid, System.nanoTime(), dxz, 0.0,
        onGround, false, sneaking, false, false, false, false, false, false, false,
        0.0, 0.0, 0.0, 50_000_000L);
  }

  @Test void noFlagForNormalSneaking() {
    assertFalse(new SafeWalkEdgeCheck(6).evaluate(state("p1", true, true, 0.05)).suspicious());
  }

  @Test void noFlagForNonSneaker() {
    assertFalse(new SafeWalkEdgeCheck(6).evaluate(state("p1", false, true, 0.3)).suspicious());
  }

  @Test void flagsForSafeWalkPattern() {
    var check = new SafeWalkEdgeCheck(3);
    for (int i = 0; i < 2; i++) check.evaluate(state("p1", true, true, 0.20));
    assertTrue(check.evaluate(state("p1", true, true, 0.20)).suspicious());
  }
}
