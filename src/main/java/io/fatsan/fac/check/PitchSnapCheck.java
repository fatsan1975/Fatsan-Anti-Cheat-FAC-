package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.RotationEvent;

public final class PitchSnapCheck extends AbstractBufferedCheck {
  public PitchSnapCheck(int limit) { super(limit); }
  @Override public String name() { return "PitchSnap"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent r)) return CheckResult.clean(name(), category());
    if (Math.abs(r.deltaPitch()) > 55.0F && Math.abs(r.deltaYaw()) < 5.0F) {
      int buf = incrementBuffer(r.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "High pitch snap with low yaw compensation", Math.min(1.0D, buf / 8.0D), false);
    } else coolDown(r.playerId());
    return CheckResult.clean(name(), category());
  }
}
