package io.fatsan.fac.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player anti-cheat exemptions for Geyser/Floodgate (Bedrock) players,
 * Citizens NPCs, and staff in spectator/vanish mode.
 *
 * <h2>Why exemptions are critical on large Folia servers</h2>
 *
 * <p>Large public servers commonly run multiple compatibility layers that produce
 * movement patterns that look like cheating but are completely legitimate:
 *
 * <h3>Geyser / Floodgate (Bedrock clients)</h3>
 * <p>Bedrock Edition clients connected via Geyser:
 * <ul>
 *   <li>Send movement packets at 20 Hz but with different float quantisation
 *       than Java clients.  Rotation checks (RotationQuantizationCheck,
 *       PitchLockCheck) will fire on many Bedrock players.</li>
 *   <li>Have a different sprint model — sprinting and sneaking state may not
 *       match Java client assumptions.</li>
 *   <li>Use different input handling — "AutoSprintAlwaysCheck" and similar
 *       input-state checks must be disabled for Bedrock players.</li>
 * </ul>
 *
 * <h3>Citizens NPCs</h3>
 * <p>Citizens plugin creates "fake player" entities that fire Bukkit events but
 * are not real players.  Without exemption, every Citizens NPC will accumulate
 * escalating violation buffers in all checks, wasting memory and triggering
 * false alerts.  NPCs should be completely bypassed.
 *
 * <h3>Staff in spectator / vanish</h3>
 * <p>Operators in spectator mode or using vanish plugins:
 * <ul>
 *   <li>Can teleport instantaneously — FreecamDesyncCheck would fire.</li>
 *   <li>Can fly — all flight checks would fire.</li>
 *   <li>Often have unlimited speed — speed checks would fire.</li>
 * </ul>
 *
 * <h2>Usage pattern</h2>
 *
 * <pre>{@code
 * // At check evaluate() entry:
 * if (exemptionService.isExempt(playerId)) {
 *   return CheckResult.clean(name(), category());
 * }
 * }</pre>
 *
 * <h2>Thread safety</h2>
 *
 * <p>All sets use {@code ConcurrentHashMap.newKeySet()} — safe from any Folia
 * region thread.
 */
public final class PlayerExemptionService {

  /** Geyser/Bedrock player UUIDs. Populated by Floodgate API on join. */
  private final Set<String> geyserPlayers = ConcurrentHashMap.newKeySet();

  /** Citizens NPC entity UUIDs. Populated by Citizens API event hook. */
  private final Set<String> npcPlayers = ConcurrentHashMap.newKeySet();

  /** Staff players currently in spectator/vanish mode. */
  private final Set<String> spectatingStaff = ConcurrentHashMap.newKeySet();

  /** Players with the FAC bypass permission node. */
  private final Set<String> permissionBypass = ConcurrentHashMap.newKeySet();

  // ── Registration ────────────────────────────────────────────────────────

  /** Register a Geyser/Bedrock player. Call from Floodgate join event. */
  public void registerGeyser(String playerId) {
    geyserPlayers.add(playerId);
  }

  /** Register a Citizens NPC UUID. Call from CitizensSpawnNPCEvent. */
  public void registerNpc(String playerId) {
    npcPlayers.add(playerId);
  }

  /** Mark a staff player as spectating/vanished. */
  public void setSpectating(String playerId, boolean spectating) {
    if (spectating) spectatingStaff.add(playerId);
    else spectatingStaff.remove(playerId);
  }

  /** Grant or revoke the permission bypass for a player. */
  public void setPermissionBypass(String playerId, boolean bypass) {
    if (bypass) permissionBypass.add(playerId);
    else permissionBypass.remove(playerId);
  }

  // ── Query ────────────────────────────────────────────────────────────────

  /**
   * Returns {@code true} if this player should be completely exempt from all
   * anti-cheat detection.
   *
   * <p>Exemption is granted when the player is:
   * <ul>
   *   <li>A Citizens NPC</li>
   *   <li>A staff player in spectator/vanish mode</li>
   *   <li>A player with the FAC bypass permission</li>
   * </ul>
   */
  public boolean isExempt(String playerId) {
    return npcPlayers.contains(playerId)
        || spectatingStaff.contains(playerId)
        || permissionBypass.contains(playerId);
  }

  /**
   * Returns {@code true} if this player is a Geyser/Bedrock player.
   *
   * <p>Bedrock players are not fully exempt — most checks still apply — but
   * rotation-quantisation and input-state checks should be skipped for them.
   * Callers can use this to conditionally disable specific checks without
   * full exemption.
   */
  public boolean isGeyser(String playerId) {
    return geyserPlayers.contains(playerId);
  }

  // ── Cleanup ──────────────────────────────────────────────────────────────

  /** Remove all exemptions for a disconnected player. */
  public void onQuit(String playerId) {
    geyserPlayers.remove(playerId);
    npcPlayers.remove(playerId);
    spectatingStaff.remove(playerId);
    permissionBypass.remove(playerId);
  }
}
