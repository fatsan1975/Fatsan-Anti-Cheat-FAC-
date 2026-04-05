package io.fatsan.fac.model;

/**
 * Block break context emitted alongside {@link BlockBreakEventSignal}.
 *
 * <p>Carries material-level data needed for checks that reason about
 * block hardness and ore type, without modifying the existing
 * {@link BlockBreakEventSignal} record (which would break 7 test files).
 *
 * <p>Checks that use this signal:
 * <ul>
 *   <li>{@code InstantBreakHardrockCheck} — blockHardness ≥ 30 with interval < 500ms</li>
 *   <li>{@code XRayHeuristicCheck} — valuable ore ratio in last 20 breaks</li>
 *   <li>{@code NukerCheck} — 3+ breaks within 100ms window</li>
 *   <li>{@code AutoFarmCheck} — crop material ratio in break history</li>
 *   <li>{@code AutoMineCheck} — uniform interval + straight mining direction</li>
 * </ul>
 */
public record BlockBreakContextSignal(
    String playerId,
    long nanoTime,
    long intervalNanos,
    /**
     * Hardness of the broken block from {@code Material.getHardness()}.
     * Negative values indicate unbreakable materials (bedrock, barriers) —
     * these should never appear here as the server cancels those break events.
     */
    float blockHardness,
    /**
     * Material enum name, e.g. "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE", "WHEAT".
     * Upper-case Bukkit Material name.
     */
    String blockMaterialName
) implements NormalizedEvent {}
