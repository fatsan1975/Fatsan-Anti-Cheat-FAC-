package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class RotationPacketPhaseLockCheck extends AbstractBufferedCheck {
  public RotationPacketPhaseLockCheck(int limit) { super(limit); }
  @Override public String name() { return "RotationPacketPhaseLock"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof RotationEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = Math.abs(e.deltaYaw())>90F && Math.abs(e.deltaPitch())>60F;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "rotation packet phase lock", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
