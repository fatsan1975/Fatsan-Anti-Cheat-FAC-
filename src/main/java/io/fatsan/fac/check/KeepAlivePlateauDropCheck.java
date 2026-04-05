package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeepAlivePlateauDropCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public KeepAlivePlateauDropCheck(int limit) { super(limit); }
  @Override public String name() { return "KeepAlivePlateauDrop"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof KeepAliveSignal keepAlive)) return CheckResult.clean(name(), category());
    boolean trigger = false;
    Long prev=last.put(keepAlive.playerId(),keepAlive.pingMillis()); if(prev!=null && prev-keepAlive.pingMillis()>120L && prev>220L) trigger=true;
    if (trigger) {
      int buf = incrementBuffer(keepAlive.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "KeepAlivePlateauDrop anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(keepAlive.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
