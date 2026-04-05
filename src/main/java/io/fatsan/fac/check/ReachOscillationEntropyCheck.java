package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReachOscillationEntropyCheck extends AbstractBufferedCheck {
  private final Map<String, Double> lastReach = new ConcurrentHashMap<>();
  private final Map<String, Integer> flips = new ConcurrentHashMap<>();
  private final Map<String, Integer> lastSign = new ConcurrentHashMap<>();

  public ReachOscillationEntropyCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "ReachOscillationEntropy";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) return CheckResult.clean(name(), category());

    Double prev = lastReach.put(hit.playerId(), hit.reachDistance());
    if (prev == null) return CheckResult.clean(name(), category());

    double diff = hit.reachDistance() - prev;
    int sign = Double.compare(diff, 0.0D);
    int previousSign = lastSign.getOrDefault(hit.playerId(), 0);
    if (Math.abs(diff) > 0.22D && previousSign != 0 && sign != 0 && sign != previousSign) {
      flips.put(hit.playerId(), flips.getOrDefault(hit.playerId(), 0) + 1);
    } else if (Math.abs(diff) < 0.08D) {
      flips.put(hit.playerId(), Math.max(0, flips.getOrDefault(hit.playerId(), 0) - 1));
    }
    if (sign != 0) lastSign.put(hit.playerId(), sign);

    if (flips.getOrDefault(hit.playerId(), 0) >= 6 && hit.reachDistance() > 3.0D) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(), "high-amplitude reach oscillation", Math.min(1.0D, buf / 7.0D), false);
      }
    } else {
      coolDown(hit.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
