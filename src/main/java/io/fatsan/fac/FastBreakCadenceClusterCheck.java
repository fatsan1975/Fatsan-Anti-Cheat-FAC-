package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockBreakEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FastBreakCadenceClusterCheck extends AbstractBufferedCheck {
  private static final long WINDOW_NANOS = 1_200_000_000L;
  private final Map<String, Deque<Long>> breakTimes = new ConcurrentHashMap<>();

  public FastBreakCadenceClusterCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "FastBreakCadenceCluster";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockBreakEventSignal blockBreak)) {
      return CheckResult.clean(name(), category());
    }

    Deque<Long> timeline = breakTimes.computeIfAbsent(blockBreak.playerId(), ignored -> new ArrayDeque<>());
    timeline.addLast(blockBreak.nanoTime());
    long cutoff = blockBreak.nanoTime() - WINDOW_NANOS;
    while (!timeline.isEmpty() && timeline.peekFirst() < cutoff) {
      timeline.removeFirst();
    }

    if (timeline.size() >= 6 && blockBreak.intervalNanos() < 120_000_000L) {
      int buf = incrementBuffer(blockBreak.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(), "Clustered fast-break cadence bursts", Math.min(1.0D, buf / 8.0D), true);
      }
    } else {
      coolDown(blockBreak.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
