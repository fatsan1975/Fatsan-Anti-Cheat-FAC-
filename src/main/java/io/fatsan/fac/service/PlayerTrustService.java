package io.fatsan.fac.service;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a lightweight per-player trust score to stabilize risk accumulation.
 *
 * <p>Trust score range is [0.0, 1.0]. High trust attenuates risk, low trust amplifies risk.
 */
public final class PlayerTrustService {
  private static final double MIN_TRUST = 0.05D;
  private static final double MAX_TRUST = 1.0D;
  private static final double INITIAL_TRUST = 0.55D;
  private static final double CLEAN_EVENT_RECOVERY = 0.004D;
  private final Map<String, Double> trustByPlayer = new ConcurrentHashMap<>();

  public void onCleanEvent(String playerId) {
    trustByPlayer.compute(
        playerId,
        (key, current) -> clamp((current == null ? INITIAL_TRUST : current) + CLEAN_EVENT_RECOVERY));
  }

  public void onSuspicion(String playerId, CheckResult result) {
    trustByPlayer.compute(
        playerId,
        (key, current) -> {
          double trust = current == null ? INITIAL_TRUST : current;
          double penalty = 0.008D + (result.severity() * categoryPenaltyWeight(result.category()));
          return clamp(trust - penalty);
        });
  }

  public double riskMultiplier(String playerId) {
    double trust = trustByPlayer.getOrDefault(playerId, INITIAL_TRUST);
    // trust=1.0 => 0.78x, trust≈0.05 => 1.35x
    return 1.4D - (0.62D * trust);
  }

  public double trustScore(String playerId) {
    return trustByPlayer.getOrDefault(playerId, INITIAL_TRUST);
  }

  /** Removes trust state for the given player. Called on disconnect. */
  public void clearPlayer(String playerId) {
    trustByPlayer.remove(playerId);
  }

  private static double categoryPenaltyWeight(CheckCategory category) {
    return switch (category) {
      case PROTOCOL -> 0.05D;
      case COMBAT -> 0.04D;
      case MOVEMENT -> 0.03D;
      case WORLD -> 0.025D;
      case INVENTORY -> 0.02D;
    };
  }

  private static double clamp(double value) {
    return Math.max(MIN_TRUST, Math.min(MAX_TRUST, value));
  }
}
