package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.TeleportSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportBurstFollowCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public TeleportBurstFollowCheck(int limit) { super(limit); }
  @Override public String name() { return "TeleportBurstFollow"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof TeleportSignal teleport)) return CheckResult.clean(name(), category());
    boolean trigger = false;
    Long prev=last.put(teleport.playerId(),teleport.nanoTime()); if(prev!=null && (teleport.nanoTime()-prev)<500_000_000L) trigger=true;
    if (trigger) {
      int buf = incrementBuffer(teleport.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "TeleportBurstFollow anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(teleport.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
