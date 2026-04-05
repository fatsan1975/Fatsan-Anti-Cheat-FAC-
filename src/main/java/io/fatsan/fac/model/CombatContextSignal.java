package io.fatsan.fac.model;

/**
 * Rich combat context emitted alongside {@link CombatHitEvent} on every hit.
 *
 * <p>Carries data that is expensive or impossible to compute inside pure check
 * logic: the arm-swing flag (requires cross-event state tracking in
 * BukkitSignalBridge), absolute positions (needed for raycast), and the
 * precomputed DDA block-intersection result (Bukkit World access is only
 * valid on the Folia region thread at event time).
 *
 * <p>Checks that use this signal:
 * <ul>
 *   <li>{@code NoSwingCheck} — swingPacketSent must be false repeatedly</li>
 *   <li>{@code ThroughWallHitCheck} — solidBlockBetween must be true repeatedly</li>
 *   <li>{@code ForcefieldCheck} — victim positions spread in all directions</li>
 *   <li>{@code BacktrackCheck} — reachDistance consistently elevated</li>
 * </ul>
 *
 * <p>Thread safety: constructed on the Folia region thread in
 * {@code BukkitSignalBridge.onHit()}. Immutable record, safe to pass anywhere.
 */
public record CombatContextSignal(
    String playerId,
    long nanoTime,
    String targetId,
    double reachDistance,
    /**
     * True if a {@code PlayerAnimateEvent} (arm swing) was received between the
     * previous hit and this one. Vanilla clients always swing before attacking.
     * Cheats that inject raw attack packets often skip the swing packet.
     */
    boolean swingPacketSent,
    double attackerX,
    double attackerY,
    double attackerZ,
    double victimX,
    double victimY,
    double victimZ,
    /**
     * True if a DDA (Digital Differential Analyzer) raycast from attacker eye
     * to victim center intersects at least one solid block. Computed eagerly in
     * BukkitSignalBridge at event time where World access is safe.
     *
     * <p>False negatives possible when block data is stale (chunk edge, high ping).
     * Checks using this field must use a buffer ≥ 2.
     */
    boolean solidBlockBetween
) implements NormalizedEvent {}
