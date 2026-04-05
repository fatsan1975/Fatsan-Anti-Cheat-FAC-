package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.TeleportSignal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportChainCheck extends AbstractBufferedCheck {
  private static final long WINDOW = 2_000_000_000L;
  private final Map<String, Deque<Long>> teleports = new ConcurrentHashMap<>();
  public TeleportChainCheck(int limit) { super(limit); }
  @Override public String name() { return "TeleportChain"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof TeleportSignal t)) return CheckResult.clean(name(), category());
    Deque<Long> w = teleports.computeIfAbsent(t.playerId(), k -> new ArrayDeque<>());
    w.addLast(t.nanoTime());
    long cutoff = t.nanoTime() - WINDOW;
    while (!w.isEmpty() && w.peekFirst() < cutoff) w.removeFirst();
    if (w.size() >= 4) {
      int buf = incrementBuffer(t.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "Dense teleport chain pattern", Math.min(1.0D, buf / 8.0D), false);
    } else coolDown(t.playerId());
    return CheckResult.clean(name(), category());
  }
}
