package io.fatsan.fac.model;

/**
 * Rich player-state snapshot emitted alongside MovementEvent on each
 * PlayerMoveEvent.  Provides context that checks on MovementEvent cannot
 * easily derive from the event itself (water, lava, eating, shield, actual
 * velocity vector).
 *
 * <p>This event is designed to feed checks that require full player context:
 * NoSlow (speed while using items), Jesus (water/lava walking), AntiKB
 * (knockback resistance), and the physics-based movement validator.
 */
public record PlayerStateEvent(
    String playerId,
    long nanoTime,
    double deltaXZ,
    double deltaY,
    boolean onGround,
    boolean sprinting,
    boolean sneaking,
    boolean eating,           // player has food/potion in use
    boolean blocking,         // player is blocking with shield
    boolean inWater,          // player is inside water block
    boolean inLava,           // player is inside lava block
    boolean climbable,        // on ladder/vine/etc.
    boolean gliding,          // player is gliding (elytra)
    boolean inVehicle,        // player is inside a vehicle
    double velocityX,         // actual server-side velocity (X)
    double velocityY,         // actual server-side velocity (Y)
    double velocityZ,         // actual server-side velocity (Z)
    long intervalNanos)
    implements NormalizedEvent {}
