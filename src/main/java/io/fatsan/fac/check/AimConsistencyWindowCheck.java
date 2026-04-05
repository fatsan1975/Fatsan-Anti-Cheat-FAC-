package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.RotationEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AimConsistencyWindowCheck extends AbstractBufferedCheck {
  private static final int WINDOW = 7;
  private final Map<String, Deque<Float>> yawWindow = new ConcurrentHashMap<>();
  public AimConsistencyWindowCheck(int limit) { super(limit); }
  @Override public String name() { return "AimConsistencyWindow"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent r)) return CheckResult.clean(name(), category());
    Deque<Float> w = yawWindow.computeIfAbsent(r.playerId(), k -> new ArrayDeque<>(WINDOW));
    w.addLast(Math.abs(r.deltaYaw()));
    if (w.size() > WINDOW) w.removeFirst();
    if (w.size() < WINDOW) return CheckResult.clean(name(), category());
    float min = w.stream().min(Float::compare).orElse(0F);
    float max = w.stream().max(Float::compare).orElse(0F);
    if (min > 8.0F && (max - min) < 1.2F) {
      int buf = incrementBuffer(r.playerId());
      if (overLimit(buf)) return new CheckResult(true, name(), category(), "Overly consistent yaw window detected", Math.min(1.0D, buf / 8.0D), false);
    } else coolDown(r.playerId());
    return CheckResult.clean(name(), category());
  }
}
