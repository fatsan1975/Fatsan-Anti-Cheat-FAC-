package io.fatsan.fac.model;

public record TrafficSignal(String playerId, long nanoTime, int eventsPerSecond, int dropped)
    implements NormalizedEvent {}
