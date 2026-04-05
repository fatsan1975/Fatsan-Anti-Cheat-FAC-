package io.fatsan.fac.model;

public record InventoryClickEventSignal(
    String playerId, long nanoTime, long intervalNanos, boolean movingFast, boolean offhandSwap)
    implements NormalizedEvent {}
