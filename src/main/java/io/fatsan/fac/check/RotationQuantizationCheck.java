package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.RotationEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RotationQuantizationCheck extends AbstractBufferedCheck {
  private static final int WINDOW_SIZE = 8;

  private final Map<String, Deque<Double>> recentYawSteps = new ConcurrentHashMap<>();

  public RotationQuantizationCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "RotationQuantization";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent rotation)) {
      return CheckResult.clean(name(), category());
    }

    double yaw = Math.abs(rotation.deltaYaw());
    if (yaw < 1.5D || yaw > 30.0D) {
      coolDown(rotation.playerId());
      return CheckResult.clean(name(), category());
    }

    Deque<Double> steps = recentYawSteps.computeIfAbsent(rotation.playerId(), ignored -> new ArrayDeque<>(WINDOW_SIZE));
    double quantized = Math.round(yaw * 100.0D) / 100.0D;
    steps.addLast(quantized);
    if (steps.size() > WINDOW_SIZE) {
      steps.removeFirst();
    }

    if (steps.size() < WINDOW_SIZE) {
      return CheckResult.clean(name(), category());
    }

    double mean = steps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
    double variance = steps.stream().mapToDouble(value -> {
      double delta = value - mean;
      return delta * delta;
    }).sum() / steps.size();

    long uniqueCount = steps.stream().distinct().count();
    if (uniqueCount <= 2 && variance < 0.04D) {
      int buf = incrementBuffer(rotation.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Highly quantized repeated rotation step pattern",
            Math.min(1.0D, buf / 8.0D),
            false);
      }
    } else {
      coolDown(rotation.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
