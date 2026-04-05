package io.fatsan.fac.model;

/**
 * Rich movement context emitted alongside {@link MovementEvent} and
 * {@link PlayerStateEvent} on every {@code PlayerMoveEvent}.
 *
 * <p>Carries data that requires Bukkit World access (only safe on the Folia
 * region thread at event time) or that needs precomputation from absolute
 * player coordinates:
 * <ul>
 *   <li>Absolute position — for LongJumpCheck, FreecamDesyncCheck, DashHackCheck</li>
 *   <li>Horizontal speed in bps — derived from deltaXZ / intervalSeconds</li>
 *   <li>insideSolidBlock — block at player feet is solid (NoclipCheck)</li>
 * </ul>
 *
 * <p>Checks that use this signal:
 * <ul>
 *   <li>{@code LongJumpCheck} — horizontal distance across a jump arc</li>
 *   <li>{@code FreecamDesyncCheck} — position delta > 20 blocks in one event</li>
 *   <li>{@code DashHackCheck} — horizontal delta > 15 blocks in one event</li>
 *   <li>{@code NoclipCheck} — insideSolidBlock while not in vehicle</li>
 *   <li>{@code AntiVoidCheck} — Y < 5 with deltaY reversal</li>
 * </ul>
 *
 * <p>Thread safety: constructed on the Folia region thread in
 * {@code BukkitSignalBridge.onMove()}. Immutable record, safe to pass anywhere.
 */
public record MovementContextSignal(
    String playerId,
    long nanoTime,
    double x,
    double y,
    double z,
    double deltaXZ,
    double deltaY,
    /**
     * Horizontal speed in blocks per second.
     * Computed as {@code deltaXZ / intervalSeconds}.
     * Zero if intervalNanos is zero or exceeds 200ms (lag guard).
     */
    double horizontalSpeedBps,
    boolean onGround,
    /**
     * True if the block at the player's feet (integer coordinates) is solid.
     * Computed via {@code player.getLocation().getBlock().getType().isSolid()}.
     * Safe to compute on the region thread.
     */
    boolean insideSolidBlock,
    boolean inWater,
    boolean inLava,
    boolean gliding,
    boolean inVehicle,
    boolean climbable,
    long intervalNanos
) implements NormalizedEvent {}
