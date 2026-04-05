package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class MovementProtocolLatencyCouplingCheck extends AbstractBufferedCheck {
  public MovementProtocolLatencyCouplingCheck(int limit) { super(limit); }
  @Override public String name() { return "MovementProtocolLatencyCoupling"; }
  @Override public CheckCategory category() { return CheckCategory.PROTOCOL; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof MovementEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = e.intervalNanos()<8_000_000L && e.deltaXZ()>0.42D;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "latency/timer coupling movement", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
