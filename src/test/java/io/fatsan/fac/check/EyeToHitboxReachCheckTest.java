package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.*;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.service.PacketContext;
import org.junit.jupiter.api.Test;

class EyeToHitboxReachCheckTest {
  private static CombatHitEvent hit(String pid, double reach) {
    return new CombatHitEvent(pid, System.nanoTime(), reach, false, true, 0f, false, false, 50_000_000L, "target1");
  }

  @Test void noFlagWhenPeUnavailable() {
    var check = new EyeToHitboxReachCheck(6, PacketContext.noop());
    assertFalse(check.evaluate(hit("p1", 3.5)).suspicious());
  }

  @Test void noFlagWhenReachNormal() {
    var ctx = PacketContext.live(); ctx.setPreciseReach("p1", 2.8);
    assertFalse(new EyeToHitboxReachCheck(6, ctx).evaluate(hit("p1", 2.8)).suspicious());
  }

  @Test void flagsWhenPreciseReachExceedsLimit() {
    var ctx = PacketContext.live(); ctx.setPreciseReach("p1", 3.6);
    var check = new EyeToHitboxReachCheck(2, ctx);
    check.evaluate(hit("p1", 3.6));
    assertTrue(check.evaluate(hit("p1", 3.6)).suspicious());
  }
}
