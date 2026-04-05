package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeepAliveFlatlineCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> flatline = new ConcurrentHashMap<>();
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public KeepAliveFlatlineCheck(int limit) { super(limit); }
  @Override public String name() { return "KeepAliveFlatline"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof KeepAliveSignal k)) return CheckResult.clean(name(), category());
    Long prev = last.put(k.playerId(), k.pingMillis());
    if (prev != null && Math.abs(prev - k.pingMillis()) <= 1L && k.pingMillis() > 250L) {
      int st = flatline.getOrDefault(k.playerId(), 0) + 1; flatline.put(k.playerId(), st);
      if (st >= 6) {
        int buf = incrementBuffer(k.playerId());
        if (overLimit(buf)) return new CheckResult(true, name(), category(), "High ping flatline keepalive pattern", Math.min(1.0D, buf / 8.0D), false);
      }
    } else { flatline.put(k.playerId(), 0); coolDown(k.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
