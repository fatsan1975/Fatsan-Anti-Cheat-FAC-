package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeepAliveSawtoothCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
private final Map<String, Integer> sign = new ConcurrentHashMap<>();
  public KeepAliveSawtoothCheck(int limit) { super(limit); }
  @Override public String name() { return "KeepAliveSawtooth"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof KeepAliveSignal keepAlive)) return CheckResult.clean(name(), category());
    boolean trigger = false;
    Long prev=last.put(keepAlive.playerId(),keepAlive.pingMillis()); if(prev!=null){ long d=keepAlive.pingMillis()-prev; Integer ps=sign.getOrDefault(keepAlive.playerId(),0); int cs=Long.compare(d,0L); if(Math.abs(d)>=60L && ps!=0 && cs!=0 && ps!=cs) trigger=true; sign.put(keepAlive.playerId(),cs);}
    if (trigger) {
      int buf = incrementBuffer(keepAlive.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "KeepAliveSawtooth anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(keepAlive.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
