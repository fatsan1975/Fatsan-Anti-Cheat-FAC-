package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;

public final class ReachHeuristicCheck extends AbstractBufferedCheck {
  public ReachHeuristicCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "ReachHeuristic";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) {
      return CheckResult.clean(name(), category());
    }
    if (hit.reachDistance() > 3.35D) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Reach distance over heuristic threshold",
            Math.min(1.0D, buf / 8.0D),
            true);
      }
    } else {
      coolDown(hit.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
