package io.fatsan.fac.model;

public record CombatHitEvent(
    String playerId,
    long nanoTime,
    double reachDistance,
    boolean criticalLike,
    boolean onGround,
    float fallDistance,
    boolean gliding,
    boolean inVehicle,
    long intervalNanos,
    String targetId)        // UUID string of the hit entity, empty if unknown
    implements NormalizedEvent {}
