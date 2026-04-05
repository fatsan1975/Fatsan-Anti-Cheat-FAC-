package io.fatsan.fac.check;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

abstract class AbstractBufferedCheck implements Check {
  private static final long PASSIVE_DECAY_STEP_NANOS = 300_000_000L;

  private final Map<String, BufferState> buffers = new ConcurrentHashMap<>();
  private final int limit;

  AbstractBufferedCheck(int limit) {
    this.limit = limit;
  }

  protected int incrementBuffer(String playerId) {
    long now = nowNanos();
    BufferState state = buffers.computeIfAbsent(playerId, key -> new BufferState(now));
    applyPassiveDecay(state, now);
    state.lastUpdateNanos = now;
    return state.value.incrementAndGet();
  }

  protected void coolDown(String playerId) {
    BufferState state = buffers.get(playerId);
    if (state == null) {
      return;
    }
    long now = nowNanos();
    applyPassiveDecay(state, now);
    state.lastUpdateNanos = now;
    int next = state.value.updateAndGet(current -> Math.max(0, current - 1));
    if (next == 0) {
      buffers.remove(playerId, state);
    }
  }

  protected boolean overLimit(int value) {
    return value >= limit;
  }

  /**
   * Removes all buffer state for the given player.
   * Called automatically by {@link io.fatsan.fac.engine.CheckRegistry#clearPlayer(String)}
   * when a player disconnects.
   */
  @Override
  public void onPlayerQuit(String playerId) {
    buffers.remove(playerId);
  }

  /** Visible for tests to deterministically control decay behavior. */
  protected long nowNanos() {
    return System.nanoTime();
  }

  private static void applyPassiveDecay(BufferState state, long nowNanos) {
    long elapsed = Math.max(0L, nowNanos - state.lastUpdateNanos);
    if (elapsed < PASSIVE_DECAY_STEP_NANOS) {
      return;
    }

    int decaySteps = (int) Math.min(8L, elapsed / PASSIVE_DECAY_STEP_NANOS);
    if (decaySteps <= 0) {
      return;
    }
    state.value.updateAndGet(current -> Math.max(0, current - decaySteps));
  }

  private static final class BufferState {
    private final AtomicInteger value = new AtomicInteger();
    private volatile long lastUpdateNanos;

    private BufferState(long nowNanos) {
      this.lastUpdateNanos = nowNanos;
    }
  }
}
