package io.fatsan.fac.model;

public record TeleportSignal(String playerId, long nanoTime) implements NormalizedEvent {}
