package io.fatsan.fac.model;

public record RotationEvent(String playerId, long nanoTime, float deltaYaw, float deltaPitch)
    implements NormalizedEvent {}
