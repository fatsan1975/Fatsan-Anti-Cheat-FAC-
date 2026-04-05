package io.fatsan.fac.model;

/**
 * Emitted by {@code BukkitSignalBridge} on {@code PlayerItemHeldEvent}
 * (hotbar slot change).
 *
 * <p>{@code intervalNanos} is the time since the previous slot change for
 * this player, or {@code Long.MAX_VALUE} on the first change in a session.
 *
 * <p>Vanilla: hotbar slot changes require physical key presses or scroll wheel
 * input, limiting rate to ~100ms minimum. Macro/script-driven slot changes
 * can occur every 1–5ms.
 *
 * <p>Used by: {@code HotbarSwapSpamCheck}
 */
public record HotbarSignal(
    String playerId,
    long nanoTime,
    int fromSlot,
    int toSlot,
    /** Nanos since last slot change; Long.MAX_VALUE on first change. */
    long intervalNanos
) implements NormalizedEvent {}
