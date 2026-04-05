package io.fatsan.fac.packet;

import io.fatsan.fac.engine.CheckRegistry;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.TrafficSignal;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PacketIntakeService {
  private static final long WINDOW_NANOS = 1_000_000_000L;
  private static final long TRAFFIC_EMIT_INTERVAL_NANOS = 250_000_000L;
  private static final long STATE_TTL_NANOS = 120_000_000_000L;

  private final CheckRegistry registry;
  private final int maxEventsPerSecond;
  private final Map<String, RateState> rates = new ConcurrentHashMap<>();
  private volatile Consumer<NormalizedEvent> consumer = event -> {};

  public PacketIntakeService(CheckRegistry registry, int maxEventsPerSecond) {
    this.registry = registry;
    this.maxEventsPerSecond = maxEventsPerSecond;
  }

  public void emit(NormalizedEvent event) {
    long now = event.nanoTime();
    RateState state = rates.computeIfAbsent(event.playerId(), key -> new RateState(now));

    if (now - state.windowStartNanos >= WINDOW_NANOS) {
      state.windowStartNanos = now;
      state.events = 0;
      state.dropped = 0;
      state.lastTrafficEmitNanos = 0L;
    }

    state.lastSeenNanos = now;
    state.events++;
    if (state.events > maxEventsPerSecond) {
      state.dropped++;
      if (state.lastTrafficEmitNanos == 0L || now - state.lastTrafficEmitNanos >= TRAFFIC_EMIT_INTERVAL_NANOS) {
        state.lastTrafficEmitNanos = now;
        consumer.accept(new TrafficSignal(event.playerId(), now, state.events, state.dropped));
      }
      return;
    }

    consumer.accept(event);
    maybeCleanupStale(now);
  }

  public void setConsumer(Consumer<NormalizedEvent> consumer) {
    this.consumer = Objects.requireNonNull(consumer, "consumer");
  }

  public CheckRegistry registry() {
    return registry;
  }

  private void maybeCleanupStale(long nowNanos) {
    if ((nowNanos & 0xFF) != 0) return; // cheap probabilistic cleanup trigger
    rates.entrySet().removeIf(entry -> nowNanos - entry.getValue().lastSeenNanos > STATE_TTL_NANOS);
  }

  private static final class RateState {
    private long windowStartNanos;
    private long lastSeenNanos;
    private long lastTrafficEmitNanos;
    private int events;
    private int dropped;

    private RateState(long nowNanos) {
      this.windowStartNanos = nowNanos;
      this.lastSeenNanos = nowNanos;
    }
  }
}
