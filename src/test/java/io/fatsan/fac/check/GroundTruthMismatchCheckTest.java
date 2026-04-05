package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.*;

import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.service.PacketContext;
import org.junit.jupiter.api.Test;

class GroundTruthMismatchCheckTest {

  private static MovementEvent move(String pid, double dxz, double dy, boolean onGround) {
    return new MovementEvent(pid, System.nanoTime(), dxz, dy, onGround, 0f, false, false, 50_000_000L);
  }

  @Test
  void noFlagWhenPeUnavailable() {
    var ctx = PacketContext.noop();
    var check = new GroundTruthMismatchCheck(6, ctx);
    CheckResult r = check.evaluate(move("p1", 0.1, 0.0, true));
    assertFalse(r.suspicious());
  }

  @Test
  void noFlagWhenGroundAgrees() {
    var ctx = PacketContext.live();
    ctx.setServerGround("p1", true);
    var check = new GroundTruthMismatchCheck(6, ctx);
    CheckResult r = check.evaluate(move("p1", 0.1, 0.0, true));
    assertFalse(r.suspicious());
  }

  @Test
  void flagsWhenClientClaimsGroundButServerSaysAir() {
    var ctx = PacketContext.live();
    ctx.setServerGround("p1", false);
    var check = new GroundTruthMismatchCheck(2, ctx);
    check.evaluate(move("p1", 0.1, 0.0, true));
    CheckResult r = check.evaluate(move("p1", 0.1, 0.0, true));
    assertTrue(r.suspicious());
  }

  @Test
  void noFlagWhenClientSaysAirAndServerSaysGround() {
    var ctx = PacketContext.live();
    ctx.setServerGround("p1", true);
    var check = new GroundTruthMismatchCheck(2, ctx);
    CheckResult r = check.evaluate(move("p1", 0.1, -0.1, false));
    assertFalse(r.suspicious());
  }
}
