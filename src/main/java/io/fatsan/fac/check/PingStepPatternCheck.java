package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PingStepPatternCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  private final Map<String, Integer> stepStreak = new ConcurrentHashMap<>();
  public PingStepPatternCheck(int limit) { super(limit); }
  @Override public String name() { return "PingStepPattern"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof KeepAliveSignal k)) return CheckResult.clean(name(), category());
    Long prev = last.put(k.playerId(), k.pingMillis());
    if (prev != null) {
      long diff = Math.abs(k.pingMillis() - prev);
      if (diff >= 48L && diff <= 56L) {
        int st = stepStreak.getOrDefault(k.playerId(), 0) + 1; stepStreak.put(k.playerId(), st);
        if (st >= 4) {
          int buf = incrementBuffer(k.playerId());
          if (overLimit(buf)) return new CheckResult(true, name(), category(), "Repeated fixed-step ping jumps", Math.min(1.0D, buf / 8.0D), false);
        }
      } else { stepStreak.put(k.playerId(), 0); coolDown(k.playerId()); }
    }
    return CheckResult.clean(name(), category());
  }
}
