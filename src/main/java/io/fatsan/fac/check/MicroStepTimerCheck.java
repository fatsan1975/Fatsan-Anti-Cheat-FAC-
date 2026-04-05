package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MicroStepTimerCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public MicroStepTimerCheck(int limit) { super(limit); }
  @Override public String name() { return "MicroStepTimer"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) return CheckResult.clean(name(), category());
    if (movement.intervalNanos() == Long.MAX_VALUE) return CheckResult.clean(name(), category());
    boolean trigger = false;
    if (movement.intervalNanos()<4_000_000L && movement.deltaXZ()<0.02D && Math.abs(movement.deltaY())<0.02D) trigger=true;
    if (trigger) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "MicroStepTimer anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(movement.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
