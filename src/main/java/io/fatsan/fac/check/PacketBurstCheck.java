package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.TrafficSignal;

public final class PacketBurstCheck extends AbstractBufferedCheck {
  public PacketBurstCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "PacketBurst";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof TrafficSignal traffic)) {
      return CheckResult.clean(name(), category());
    }

    if (traffic.eventsPerSecond() > 500 || traffic.dropped() > 25) {
      int buf = incrementBuffer(traffic.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Traffic burst / flood behavior",
            Math.min(1.0D, buf / 6.0D),
            true);
      }
    }

    return CheckResult.clean(name(), category());
  }
}
