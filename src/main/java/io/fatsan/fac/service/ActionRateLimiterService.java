package io.fatsan.fac.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Prevents repetitive alert/action spam for the same player in short windows. */
public final class ActionRateLimiterService {
  private final long alertCooldownMillis;
  private final long setbackCooldownMillis;
  private final long kickCooldownMillis;
  private final LongSupplier nowMillis;

  private final Map<String, Long> lastAlertAt = new ConcurrentHashMap<>();
  private final Map<String, Long> lastSetbackAt = new ConcurrentHashMap<>();
  private final Map<String, Long> lastKickAt = new ConcurrentHashMap<>();

  public ActionRateLimiterService(
      long alertCooldownMillis, long setbackCooldownMillis, long kickCooldownMillis) {
    this(alertCooldownMillis, setbackCooldownMillis, kickCooldownMillis, System::currentTimeMillis);
  }

  public ActionRateLimiterService(
      long alertCooldownMillis,
      long setbackCooldownMillis,
      long kickCooldownMillis,
      LongSupplier nowMillis) {
    this.alertCooldownMillis = Math.max(0L, alertCooldownMillis);
    this.setbackCooldownMillis = Math.max(0L, setbackCooldownMillis);
    this.kickCooldownMillis = Math.max(0L, kickCooldownMillis);
    this.nowMillis = nowMillis;
  }

  public boolean allowAlert(String playerId) {
    return tryAcquire(lastAlertAt, playerId, alertCooldownMillis);
  }

  public boolean allowSetback(String playerId) {
    return tryAcquire(lastSetbackAt, playerId, setbackCooldownMillis);
  }

  public boolean allowKick(String playerId) {
    return tryAcquire(lastKickAt, playerId, kickCooldownMillis);
  }

  private boolean tryAcquire(Map<String, Long> map, String playerId, long cooldownMillis) {
    long now = nowMillis.getAsLong();
    final boolean[] granted = {false};
    map.compute(
        playerId,
        (key, previous) -> {
          if (previous != null && (now - previous) < cooldownMillis) {
            return previous;
          }
          granted[0] = true;
          return now;
        });
    return granted[0];
  }

  /** Removes all cooldown state for the given player. Called on disconnect. */
  public void clearPlayer(String playerId) {
    lastAlertAt.remove(playerId);
    lastSetbackAt.remove(playerId);
    lastKickAt.remove(playerId);
  }
}
