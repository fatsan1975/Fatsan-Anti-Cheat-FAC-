package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LandingBouncePatternCheck extends AbstractBufferedCheck {
  private final Map<String, Boolean> lastGround = new ConcurrentHashMap<>();
  public LandingBouncePatternCheck(int limit) { super(limit); }
  @Override public String name() { return "LandingBouncePattern"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) return CheckResult.clean(name(), category());
    boolean trigger = false;
    Boolean prevG=lastGround.put(movement.playerId(), movement.onGround()); if (prevG!=null && prevG && !movement.onGround() && movement.deltaY()>0.42D) trigger=true;
    if (trigger) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "LandingBouncePattern anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(movement.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
