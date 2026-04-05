package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReachSpikeClusterCheck extends AbstractBufferedCheck {
  private static final long WINDOW_NANOS = 800_000_000L;

  private final Map<String, Deque<Long>> spikeTimeline = new ConcurrentHashMap<>();

  public ReachSpikeClusterCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "ReachSpikeCluster";
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

    if (hit.reachDistance() <= 3.25D) {
      coolDown(hit.playerId());
      return CheckResult.clean(name(), category());
    }

    Deque<Long> timeline = spikeTimeline.computeIfAbsent(hit.playerId(), ignored -> new ArrayDeque<>());
    timeline.addLast(hit.nanoTime());
    long cutoff = hit.nanoTime() - WINDOW_NANOS;
    while (!timeline.isEmpty() && timeline.peekFirst() < cutoff) {
      timeline.removeFirst();
    }

    if (timeline.size() >= 3) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Clustered high-reach spikes within short combat window",
            Math.min(1.0D, buf / 8.0D),
            true);
      }
    }

    return CheckResult.clean(name(), category());
  }
}
