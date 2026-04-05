package io.fatsan.fac.model;

public record MovementEvent(
    String playerId,
    long nanoTime,
    double deltaXZ,
    double deltaY,
    boolean onGround,
    float fallDistance,
    boolean gliding,
    boolean inVehicle,
    long intervalNanos)
    implements NormalizedEvent {}
