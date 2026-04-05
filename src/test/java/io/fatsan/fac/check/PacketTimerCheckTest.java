package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.*;

import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.service.PacketContext;
import io.fatsan.fac.service.PacketTimerService;
import org.junit.jupiter.api.Test;

class PacketTimerCheckTest {

  private static MovementEvent move(String pid) {
    return new MovementEvent(pid, System.nanoTime(), 0.1, 0.0, true, 0f, false, false, 50_000_000L);
  }

  @Test
  void noFlagWhenPeUnavailable() {
    var ctx = PacketContext.noop();
    var timer = new PacketTimerService();
    var check = new PacketTimerCheck(6, ctx, timer);
    CheckResult r = check.evaluate(move("p1"));
    assertFalse(r.suspicious());
  }

  @Test
  void noFlagWhenTimerNormal() {
    var ctx = PacketContext.live();
    var timer = new PacketTimerService();
    long now = System.nanoTime();
    for (int i = 0; i < 60; i++) {
      timer.onFlyingPacket("p1", now + i * 50_000_000L);
    }
    var check = new PacketTimerCheck(6, ctx, timer);
    CheckResult r = check.evaluate(move("p1"));
    assertFalse(r.suspicious());
  }

  @Test
  void flagsWhenTimerFast() {
    var ctx = PacketContext.live();
    var timer = new PacketTimerService();
    long now = System.nanoTime();
    for (int i = 0; i < 60; i++) {
      timer.onFlyingPacket("p1", now + i * 40_000_000L);
    }
    var check = new PacketTimerCheck(2, ctx, timer);
    check.evaluate(move("p1"));
    CheckResult r = check.evaluate(move("p1"));
    assertTrue(r.suspicious());
  }

  @Test
  void clearsOnQuit() {
    var ctx = PacketContext.live();
    var timer = new PacketTimerService();
    var check = new PacketTimerCheck(6, ctx, timer);
    check.evaluate(move("p1"));
    check.onPlayerQuit("p1");
    CheckResult r = check.evaluate(move("p1"));
    assertFalse(r.suspicious());
  }
}
