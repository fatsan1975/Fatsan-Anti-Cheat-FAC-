package io.fatsan.fac.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tick-level timer detection using cumulative balance tracking.
 * Legitimate client: 20 flying packets/sec (50ms apart).
 * Timer hack: faster rate accumulates positive balance.
 * Detects even subtle 1.01x-1.05x timer speeds.
 */
public final class PacketTimerService {
  private static final long EXPECTED_TICK_NANOS = 50_000_000L;
  private static final long VIOLATION_THRESHOLD_NANOS = 150_000_000L;
  private static final long MAX_TICK_DELTA_NANOS = 200_000_000L;
  private static final long MIN_TICK_DELTA_NANOS = 5_000_000L;

  private final Map<String, TimerState> states = new ConcurrentHashMap<>();

  public void onFlyingPacket(String playerId, long nanoTime) {
    states.compute(playerId, (key, state) -> {
      if (state == null) return new TimerState(nanoTime, 0L, 0);
      long delta = nanoTime - state.lastPacketNano;
      if (delta < MIN_TICK_DELTA_NANOS || delta > MAX_TICK_DELTA_NANOS) {
        return new TimerState(nanoTime, state.balance, state.packetCount + 1);
      }
      long newBalance = state.balance + (EXPECTED_TICK_NANOS - delta);
      return new TimerState(nanoTime, newBalance, state.packetCount + 1);
    });
  }

  public long getBalance(String playerId) {
    TimerState state = states.get(playerId);
    return state != null ? state.balance : 0L;
  }

  public boolean isTimerViolation(String playerId) {
    TimerState state = states.get(playerId);
    if (state == null || state.packetCount < 40) return false;
    return Math.abs(state.balance) > VIOLATION_THRESHOLD_NANOS;
  }

  public boolean isFastTimer(String playerId) {
    TimerState state = states.get(playerId);
    if (state == null || state.packetCount < 40) return false;
    return state.balance > VIOLATION_THRESHOLD_NANOS;
  }

  public void clearPlayer(String playerId) { states.remove(playerId); }

  private record TimerState(long lastPacketNano, long balance, int packetCount) {}
}
