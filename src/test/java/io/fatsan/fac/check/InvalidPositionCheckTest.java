package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.*;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.service.PacketContext;
import org.junit.jupiter.api.Test;

class InvalidPositionCheckTest {
  private static MovementEvent move(String pid, double dxz, double dy) {
    return new MovementEvent(pid, System.nanoTime(), dxz, dy, true, 0f, false, false, 50_000_000L);
  }

  @Test void noFlagWhenPeUnavailable() {
    assertFalse(new InvalidPositionCheck(6, PacketContext.noop()).evaluate(move("p1", 0.1, 0.0)).suspicious());
  }

  @Test void noFlagWhenNoLastPosition() {
    assertFalse(new InvalidPositionCheck(6, PacketContext.live()).evaluate(move("p1", 0.1, 0.0)).suspicious());
  }

  @Test void flagsWhenDeltaExceedsMax() {
    var ctx = PacketContext.live();
    ctx.setLastValidPosition("p1", 0.0, 64.0, 0.0);
    var check = new InvalidPositionCheck(2, ctx);
    check.evaluate(move("p1", 20.0, 0.0));
    assertTrue(check.evaluate(move("p1", 20.0, 0.0)).suspicious());
  }
}
