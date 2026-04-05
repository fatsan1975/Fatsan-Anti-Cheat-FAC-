package io.fatsan.fac.model;

public record BlockPlaceEventSignal(
    String playerId, long nanoTime, long intervalNanos, boolean sprinting, double horizontalSpeed,
    String itemTypeKey)
    implements NormalizedEvent {}
