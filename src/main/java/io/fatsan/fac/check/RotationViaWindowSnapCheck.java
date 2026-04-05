package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class RotationViaWindowSnapCheck extends AbstractBufferedCheck {
  public RotationViaWindowSnapCheck(int limit) { super(limit); }
  @Override public String name() { return "RotationViaWindowSnap"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof RotationEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = Math.abs(e.deltaYaw())>160F && Math.abs(e.deltaPitch())<0.5F;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"rotation via window snap",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
