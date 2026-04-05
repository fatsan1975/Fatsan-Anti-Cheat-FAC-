package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.RotationEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RotationDirectionLockCheck extends AbstractBufferedCheck {
  private final Map<String, Integer> lastSign = new ConcurrentHashMap<>();
private final Map<String, Integer> streak = new ConcurrentHashMap<>();
  public RotationDirectionLockCheck(int limit) { super(limit); }
  @Override public String name() { return "RotationDirectionLock"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent rotation)) return CheckResult.clean(name(), category());
    boolean trigger = false;
    float y=rotation.deltaYaw(); Integer sign=lastSign.get(rotation.playerId()); int cur=Float.compare(y,0F); if (Math.abs(y)>15.0F && sign!=null && sign==cur) {int st=streak.getOrDefault(rotation.playerId(),0)+1; streak.put(rotation.playerId(),st); if(st>=7) trigger=true;} else {streak.put(rotation.playerId(),0);} lastSign.put(rotation.playerId(),cur);
    if (trigger) {
      int buf = incrementBuffer(rotation.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "RotationDirectionLock anomaly pattern", Math.min(1.0D, buf / 8.0D), false);
    } else { coolDown(rotation.playerId()); }
    return CheckResult.clean(name(), category());
  }
}
