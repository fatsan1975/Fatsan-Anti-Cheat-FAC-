package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.RotationEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RotationModuloPatternCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public RotationModuloPatternCheck(int limit) { super(limit); }
  @Override public String name() { return "RotationModuloPattern"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent rotation)) return CheckResult.clean(name(), category());
    boolean trigger = false;
    float y=Math.abs(rotation.deltaYaw()); if (y>8.0F) { float mod=y%3.0F; if (mod<0.06F || mod>2.94F) trigger=true; }
    if (trigger) {
      int buf = incrementBuffer(rotation.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "RotationModuloPattern anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(rotation.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
