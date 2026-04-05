package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class RotationViaSensitivityDriftCheck extends AbstractBufferedCheck {
  public RotationViaSensitivityDriftCheck(int limit) { super(limit); }
  @Override public String name() { return "RotationViaSensitivityDrift"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof RotationEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = Math.abs(e.deltaYaw())>150F && Math.abs(e.deltaPitch())<0.03F;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "via sensitivity drift", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
