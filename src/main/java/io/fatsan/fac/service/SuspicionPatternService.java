package io.fatsan.fac.service;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Tracks short-term suspicion patterns to amplify risk for bursty/repetitive behavior.
 */
public final class SuspicionPatternService {
  private static final long STREAK_WINDOW_MILLIS = 4_000L;
  private static final long RECENT_HALF_LIFE_MILLIS = 6_000L;
  private final LongSupplier nowMillis;
  private final Map<String, State> states = new ConcurrentHashMap<>();

  public SuspicionPatternService() {
    this(System::currentTimeMillis);
  }

  public SuspicionPatternService(LongSupplier nowMillis) {
    this.nowMillis = nowMillis;
  }

  public double onSuspicion(String playerId, CheckResult result) {
    long now = nowMillis.getAsLong();
    State state = states.computeIfAbsent(playerId, key -> new State(now));
    state.decayRecent(now);

    if (state.lastCategory == result.category() && now - state.lastSeenAt <= STREAK_WINDOW_MILLIS) {
      state.categoryStreak++;
    } else {
      state.categoryStreak = 1;
      state.lastCategory = result.category();
    }

    state.lastSeenAt = now;
    state.recentIntensity += 1.0D;

    double streakBoost = Math.min(0.28D, (state.categoryStreak - 1) * 0.04D);
    double burstBoost = Math.min(0.22D, state.recentIntensity * 0.03D);
    double categoryBias = categoryBias(result.category());
    return 1.0D + streakBoost + burstBoost + categoryBias;
  }

  public void onCleanEvent(String playerId) {
    State state = states.get(playerId);
    if (state == null) {
      return;
    }
    long now = nowMillis.getAsLong();
    state.decayRecent(now);
    state.recentIntensity = Math.max(0.0D, state.recentIntensity - 0.15D);
    if (state.recentIntensity <= 0.2D) {
      state.categoryStreak = Math.max(0, state.categoryStreak - 1);
    }
  }


  public double recentIntensity(String playerId) {
    State state = states.get(playerId);
    if (state == null) {
      return 0.0D;
    }
    long now = nowMillis.getAsLong();
    state.decayRecent(now);
    return state.recentIntensity;
  }

  /** Removes all suspicion pattern state for the given player. Called on disconnect. */
  public void clearPlayer(String playerId) {
    states.remove(playerId);
  }
  private static double categoryBias(CheckCategory category) {
    return switch (category) {
      case COMBAT -> 0.06D;
      case PROTOCOL -> 0.05D;
      case MOVEMENT -> 0.04D;
      case WORLD -> 0.02D;
      case INVENTORY -> 0.01D;
    };
  }

  private static final class State {
    private long lastSeenAt;
    private CheckCategory lastCategory;
    private int categoryStreak;
    private double recentIntensity;

    private State(long now) {
      this.lastSeenAt = now;
    }

    private void decayRecent(long now) {
      long elapsed = Math.max(0L, now - lastSeenAt);
      if (elapsed == 0L || recentIntensity <= 0.0D) {
        return;
      }
      double decayFactor = Math.pow(0.5D, (double) elapsed / RECENT_HALF_LIFE_MILLIS);
      recentIntensity *= decayFactor;
    }
  }
}
