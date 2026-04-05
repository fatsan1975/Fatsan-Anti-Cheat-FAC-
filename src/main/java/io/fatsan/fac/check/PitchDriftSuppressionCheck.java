package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.RotationEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PitchDriftSuppressionCheck extends AbstractBufferedCheck {
  private final Map<String, Long> last = new ConcurrentHashMap<>();
  public PitchDriftSuppressionCheck(int limit) { super(limit); }
  @Override public String name() { return "PitchDriftSuppression"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent rotation)) return CheckResult.clean(name(), category());
    boolean trigger = false;
    if (Math.abs(rotation.deltaYaw())>35.0F && Math.abs(rotation.deltaPitch())<0.03F) trigger=true;
    if (trigger) {
      int buf = incrementBuffer(rotation.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "PitchDriftSuppression anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(rotation.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
