package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.service.PacketContext;
import io.fatsan.fac.service.PacketTimerService;

/**
 * Detects timer hacks (1.01x-1.5x+) using cumulative balance tracking
 * from PacketTimerService. When PacketEvents is unavailable, this check
 * is a no-op (TimerFrequencyCheck handles PPS-based detection).
 */
public final class PacketTimerCheck extends AbstractBufferedCheck {

  private final PacketContext packetContext;
  private final PacketTimerService timerService;

  public PacketTimerCheck(int limit, PacketContext packetContext, PacketTimerService timerService) {
    super(limit);
    this.packetContext = packetContext;
    this.timerService = timerService;
  }

  @Override
  public String name() {
    return "PacketTimer";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) {
      return CheckResult.clean(name(), category());
    }

    if (!packetContext.isPacketEventsAvailable()) {
      return CheckResult.clean(name(), category());
    }

    if (timerService.isFastTimer(movement.playerId())) {
      long balance = timerService.getBalance(movement.playerId());
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(),
            "Timer speed violation (balance="
                + String.format("%.1f", balance / 1_000_000.0)
                + "ms)",
            Math.min(1.0, buf / 8.0),
            true);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
