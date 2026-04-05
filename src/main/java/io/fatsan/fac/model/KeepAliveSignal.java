package io.fatsan.fac.model;

public record KeepAliveSignal(String playerId, long nanoTime, long pingMillis) implements NormalizedEvent {}
