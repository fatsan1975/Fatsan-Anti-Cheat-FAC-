package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.TeleportSignal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportOrderHeuristicCheck extends AbstractBufferedCheck {
  private final Map<String, Long> lastTeleport = new ConcurrentHashMap<>();

  public TeleportOrderHeuristicCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "TeleportOrderHeuristic";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (event instanceof TeleportSignal teleport) {
      lastTeleport.put(teleport.playerId(), teleport.nanoTime());
      return CheckResult.clean(name(), category());
    }

    if (event instanceof MovementEvent movement) {
      Long tp = lastTeleport.get(movement.playerId());
      if (tp == null) {
        return CheckResult.clean(name(), category());
      }
      long deltaMs = (movement.nanoTime() - tp) / 1_000_000L;
      if (deltaMs >= 0 && deltaMs < 5 && movement.deltaXZ() > 3.2D) {
        int buf = incrementBuffer(movement.playerId());
        if (overLimit(buf)) {
          return new CheckResult(
              true,
              name(),
              category(),
              "Post-teleport movement desync pattern",
              Math.min(1.0D, buf / 7.0D),
              false);
        }
      } else {
        coolDown(movement.playerId());
      }
    }

    return CheckResult.clean(name(), category());
  }
}
