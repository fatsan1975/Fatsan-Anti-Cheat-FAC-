package io.fatsan.fac.service;

import io.fatsan.fac.model.CheckResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RiskService {
  private static final double DECAY = 0.92D;
  /** Minimum risk contribution per violation, prevents zero-severity noise from being free. */
  private static final double MIN_CONTRIBUTION = 0.15D;

  private final Map<String, Double> riskByPlayer = new ConcurrentHashMap<>();

  public double apply(String playerId, CheckResult result, double trustMultiplier) {
    // Normalise severity through the family cap before accumulating risk.
    // This prevents noisy statistical / via-derived families from dominating
    // the risk score at the same rate as high-confidence core checks.
    double normalizedSeverity =
        SeverityNormalizer.normalize(result.checkName(), result.category(), result.severity());

    return riskByPlayer.compute(
        playerId,
        (key, current) -> {
          double base = current == null ? 0.0D : current * DECAY;
          return base + (Math.max(MIN_CONTRIBUTION, normalizedSeverity * weight(result)) * trustMultiplier);
        });
  }

  public void coolDown(String playerId) {
    riskByPlayer.computeIfPresent(playerId, (key, risk) -> Math.max(0.0D, risk * 0.9D - 0.04D));
  }

  public double currentRisk(String playerId) {
    return riskByPlayer.getOrDefault(playerId, 0.0D);
  }

  /** Removes all accumulated risk for the given player. Called on disconnect. */
  public void clearPlayer(String playerId) {
    riskByPlayer.remove(playerId);
  }
  private static double weight(CheckResult result) {
    return switch (result.category()) {
      case PROTOCOL -> 1.4D;
      case MOVEMENT -> 1.2D;
      case COMBAT -> 1.3D;
      case WORLD -> 1.1D;
      case INVENTORY -> 0.9D;
    };
  }
}
