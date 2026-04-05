package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.service.PacketContext;

/**
 * Detects impossible position transitions using PE-validated last position.
 * Flags when horizontal delta per tick exceeds what's physically possible.
 * When PE unavailable, no-op.
 */
public final class InvalidPositionCheck extends AbstractBufferedCheck {
  private static final double MAX_HORIZONTAL_PER_TICK = 2.5;
  private static final double MAX_VERTICAL_PER_TICK = 4.0;
  private final PacketContext packetContext;

  public InvalidPositionCheck(int limit, PacketContext packetContext) {
    super(limit);
    this.packetContext = packetContext;
  }

  @Override public String name() { return "InvalidPosition"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) return CheckResult.clean(name(), category());
    if (!packetContext.isPacketEventsAvailable()) return CheckResult.clean(name(), category());
    double[] lastPos = packetContext.getLastValidPosition(movement.playerId());
    if (lastPos == null) return CheckResult.clean(name(), category());

    double seconds = movement.intervalNanos() / 1_000_000_000.0;
    if (seconds <= 0.0 || seconds > 0.15) return CheckResult.clean(name(), category());

    double tickFraction = seconds / 0.05;
    double maxH = MAX_HORIZONTAL_PER_TICK * Math.max(1.0, tickFraction);
    double maxV = MAX_VERTICAL_PER_TICK * Math.max(1.0, tickFraction);

    if (movement.deltaXZ() > maxH || Math.abs(movement.deltaY()) > maxV) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(),
            "Impossible position delta (dxz=" + String.format("%.2f", movement.deltaXZ())
                + " dy=" + String.format("%.2f", movement.deltaY()) + ")",
            Math.min(1.0, buf / 4.0), true);
      }
    } else {
      coolDown(movement.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
