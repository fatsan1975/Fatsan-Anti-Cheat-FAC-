package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.RotationEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AimAssist Detection (151) - Smooth Aim
 * Yumuşak aim tespiti - İnsan mümkün olmayan yumuşak rotasyon değişiklikleri
 */
public class AimAssistSmoothCheck implements Check {
  private final int bufferLimit;
  private final Map<String, RotationHistory> histories = new ConcurrentHashMap<>();

  public AimAssistSmoothCheck(int bufferLimit) {
    this.bufferLimit = bufferLimit;
  }

  @Override
  public String name() {
    return "AimAssistSmooth";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof RotationEvent rot)) {
      return CheckResult.clean(name(), category());
    }

    RotationHistory history = histories.computeIfAbsent(rot.playerId(), k -> new RotationHistory());

    float yawDelta = Math.abs(rot.deltaYaw());
    float pitchDelta = Math.abs(rot.deltaPitch());

    // Skip small movements
    if (yawDelta < 0.5f && pitchDelta < 0.5f) {
      return CheckResult.clean(name(), category());
    }

    // Check for smooth aim patterns
    double smoothness = calculateSmoothness(history, yawDelta, pitchDelta);

    history.addSample(yawDelta, pitchDelta, rot.nanoTime());

    // AimAssist typically has very consistent smoothness
    if (smoothness > 0.95 && smoothness < 1.05) {
      return new CheckResult(true, name(), category(), "AimAssist smooth aim detected (smoothness=" + String.format("%.3f", smoothness) + ")", 0.75, false);
    }

    // Check for micro-adjustments (152)
    if (detectMicroAdjustments(history)) {
      return new CheckResult(true, name(), category(), "AimAssist micro-adjustments detected", 0.65, false);
    }

    return CheckResult.clean(name(), category());
  }

  private double calculateSmoothness(RotationHistory history, float yawDelta, float pitchDelta) {
    if (history.samples.size() < 5) {
      return 0;
    }

    // Calculate variance in rotation deltas
    double avgYawDelta = history.samples.stream()
        .mapToDouble(s -> s.yawDelta)
        .average()
        .orElse(0);

    double variance = history.samples.stream()
        .mapToDouble(s -> Math.abs(s.yawDelta - avgYawDelta))
        .average()
        .orElse(0);

    // Smooth aim has very low variance
    if (avgYawDelta > 0) {
      return 1.0 - (variance / avgYawDelta);
    }

    return 0;
  }

  private boolean detectMicroAdjustments(RotationHistory history) {
    if (history.samples.size() < 10) {
      return false;
    }

    // Count micro-adjustments (very small, precise movements)
    int microAdjustments = 0;
    for (RotationSample sample : history.samples) {
      if (sample.yawDelta > 0.1 && sample.yawDelta < 1.0) {
        microAdjustments++;
      }
    }

    // More than 70% micro-adjustments is suspicious
    return (double) microAdjustments / history.samples.size() > 0.7;
  }

  @Override
  public void onPlayerQuit(String playerId) {
    histories.remove(playerId);
  }

  private static class RotationHistory {
    private final java.util.List<RotationSample> samples = new java.util.ArrayList<>();

    void addSample(float yawDelta, float pitchDelta, long time) {
      samples.add(new RotationSample(yawDelta, pitchDelta, time));
      if (samples.size() > 20) {
        samples.remove(0);
      }
    }
  }

  private static class RotationSample {
    final float yawDelta;
    final float pitchDelta;
    final long time;

    RotationSample(float yawDelta, float pitchDelta, long time) {
      this.yawDelta = yawDelta;
      this.pitchDelta = pitchDelta;
      this.time = time;
    }
  }
}
