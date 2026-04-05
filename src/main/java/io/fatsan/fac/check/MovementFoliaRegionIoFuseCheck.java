package io.fatsan.fac.check;

import io.fatsan.fac.model.*;

public final class MovementFoliaRegionIoFuseCheck extends AbstractBufferedCheck {
  public MovementFoliaRegionIoFuseCheck(int limit) { super(limit); }
  @Override public String name() { return "MovementFoliaRegionIoFuse"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }
  @Override public CheckResult evaluate(NormalizedEvent event) {
    if(!(event instanceof MovementEvent e)) return CheckResult.clean(name(), category());
    boolean trigger = e.intervalNanos()>90_000_000L && e.deltaXZ()>0.46D && e.onGround();
    if(trigger){ int b=incrementBuffer(e.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"folia region io fuse",Math.min(1D,b/7D),true); }
    else coolDown(e.playerId());
    return CheckResult.clean(name(), category());
  }
}
