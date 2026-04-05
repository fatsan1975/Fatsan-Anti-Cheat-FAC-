package io.fatsan.fac.model;

public record BlockBreakEventSignal(
    String playerId,
    long nanoTime,
    long intervalNanos,
    int efficiencyLevel,
    int hasteAmplifier,
    int miningFatigueAmplifier,
    double attackSpeedAttribute,
    double movementSpeedAttribute,
    String itemTypeKey,
    double itemAttackSpeedBonus,
    double itemMovementSpeedBonus,
    int enchantWeight,
    boolean customItemContext)
    implements NormalizedEvent {}
