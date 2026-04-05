package io.fatsan.fac.model;

/**
 * Emitted by {@code BukkitSignalBridge} on {@code PlayerFishEvent} with
 * state {@code CAUGHT_FISH} — i.e., when a player reels in a catch.
 *
 * <p>{@code intervalNanos} is the time since the previous CAUGHT_FISH event
 * for this player, or {@code Long.MAX_VALUE} on the first catch in a session.
 *
 * <p>Vanilla fishing wait time is uniformly random between 5–30 seconds.
 * AutoFish bots detect the bobber splash (client-side) and reel in
 * immediately, producing catches every 100–500ms.
 *
 * <p>Used by: {@code AutoFishCheck}
 */
public record FishingSignal(
    String playerId,
    long nanoTime,
    /** Nanos since last CAUGHT_FISH; Long.MAX_VALUE on first catch. */
    long intervalNanos
) implements NormalizedEvent {}
