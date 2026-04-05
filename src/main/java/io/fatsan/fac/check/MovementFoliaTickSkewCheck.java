package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class MovementFoliaTickSkewCheck extends AbstractBufferedCheck {
  public MovementFoliaTickSkewCheck(int limit) { super(limit); }
  @Override public String name() { return "MovementFoliaTickSkew"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof MovementEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = e.intervalNanos()>120_000_000L && e.deltaXZ()>0.5D;
    if(trigger){
      int buffer = incrementBuffer(e.playerId());
      if(overLimit(buffer)) return new CheckResult(true, name(), category(), "folia tick skew movement burst", Math.min(1D, buffer/7D), true);
    } else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
