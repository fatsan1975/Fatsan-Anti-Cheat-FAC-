package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class MovementViaSmoothingMismatchCheck extends AbstractBufferedCheck {
  public MovementViaSmoothingMismatchCheck(int limit) { super(limit); }
  @Override public String name() { return "MovementViaSmoothingMismatch"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof MovementEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = e.intervalNanos()<11_000_000L && e.deltaY()==0.0D && e.deltaXZ()>0.41D;
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"via smoothing mismatch",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
