package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.InventoryClickEventSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryMovingEntropyCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public InventoryMovingEntropyCheck(int limit) { super(limit); }
  @Override public String name() { return "InventoryMovingEntropy"; }
  @Override public CheckCategory category() { return CheckCategory.INVENTORY; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof InventoryClickEventSignal click)) return CheckResult.clean(name(), category());
    if (click.intervalNanos() == Long.MAX_VALUE) return CheckResult.clean(name(), category());
    boolean trigger = false;
    long cur=click.intervalNanos()/1_000_000L; if(click.movingFast()){Long prev=last.put(click.playerId(),cur); if(prev!=null && Math.abs(prev-cur)<=2L && cur<100L) trigger=true;}
    if (trigger) {
      int buf = incrementBuffer(click.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "InventoryMovingEntropy anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(click.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
