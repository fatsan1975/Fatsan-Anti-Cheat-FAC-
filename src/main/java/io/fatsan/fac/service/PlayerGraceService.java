package io.fatsan.fac.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player grace periods during which anti-cheat checks should be
 * suppressed or scored at reduced confidence.
 *
 * <h2>Why grace periods are necessary on real Folia servers</h2>
 *
 * <p>On large Folia servers with hundreds of players, several common events
 * produce movement and combat data that looks exactly like cheating but is
 * completely legitimate:
 *
 * <ul>
 *   <li><b>Login:</b> Players spawn inside blocks, their client loads chunks
 *       late, and their initial movement packets can have huge deltas and
 *       insane speeds.  Without a grace period, every player triggers
 *       FlightSustainedCheck, NoclipCheck, and DashHackCheck on join.</li>
 *   <li><b>Server teleports / setbacks:</b> When the anti-cheat itself issues
 *       a setback (corrective teleport), the next 2–5 movement packets will
 *       appear to have abnormal deltas.  Without suppression, the setback
 *       causes a second flag, which causes a second setback, creating an
 *       infinite loop.</li>
 *   <li><b>Game mode changes (creative/spectator):</b> Creative flight is
 *       vanilla — it should not trigger FlightSustainedCheck.</li>
 *   <li><b>Teleport events:</b> Plugins like Essentials, WorldEdit, and
 *       Folia region transitions teleport players legitimately.  FreecamDesyncCheck
 *       and DashHackCheck must be suppressed for the next 3 packets.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 *
 * <p>All state is stored in {@code ConcurrentHashMap} keyed by player UUID
 * string.  Grace expiry times are absolute nanoseconds from
 * {@code System.nanoTime()}.  Reads and writes are safe from any Folia region
 * thread.
 *
 * <h2>Usage pattern</h2>
 *
 * <pre>{@code
 * // At the start of a check's evaluate():
 * if (graceService.isUnderGrace(playerId)) {
 *   return CheckResult.clean(name(), category());
 * }
 * }</pre>
 *
 * <p>Checks that should always run even during grace (e.g., ChatPacketAnomalyCheck)
 * should NOT consult this service.
 */
public final class PlayerGraceService {

  /**
   * Grace duration granted on login/respawn (ms).
   * Covers chunk-loading lag and initial position desync.
   */
  private static final long LOGIN_GRACE_MS = 5_000;

  /**
   * Grace duration after a server-issued setback teleport (ms).
   * Covers the 2–3 movement packets that follow a correction.
   */
  private static final long SETBACK_GRACE_MS = 1_500;

  /**
   * Grace duration after any plugin teleport (Essentials, WorldEdit, Folia
   * region transition, etc.) (ms).
   */
  private static final long TELEPORT_GRACE_MS = 2_000;

  /**
   * Grace duration after a game mode change to CREATIVE or SPECTATOR (ms).
   * Suppresses flight checks for the entire creative session via isCreative flag.
   */
  private static final long GAMEMODE_GRACE_MS = 500;

  /** Per-player grace expiry time in nanoseconds. */
  private final Map<String, Long> graceUntilNanos = new ConcurrentHashMap<>();

  /** Players currently in creative or spectator mode — flight is always exempt. */
  private final java.util.Set<String> creativePlayers =
      ConcurrentHashMap.newKeySet();

  /** Record login / respawn grace for a player. */
  public void onLogin(String playerId) {
    grantGrace(playerId, LOGIN_GRACE_MS);
  }

  /** Record that the anti-cheat issued a setback for this player. */
  public void onSetback(String playerId) {
    grantGrace(playerId, SETBACK_GRACE_MS);
  }

  /** Record a plugin/server teleport (Folia region transfer, command, etc.). */
  public void onTeleport(String playerId) {
    grantGrace(playerId, TELEPORT_GRACE_MS);
  }

  /** Record a game mode change to creative or spectator. */
  public void onCreativeMode(String playerId) {
    creativePlayers.add(playerId);
    grantGrace(playerId, GAMEMODE_GRACE_MS);
  }

  /** Record a game mode change away from creative/spectator to survival/adventure. */
  public void onSurvivalMode(String playerId) {
    creativePlayers.remove(playerId);
    grantGrace(playerId, GAMEMODE_GRACE_MS);
  }

  /**
   * Returns {@code true} if the player is currently within a grace period and
   * movement/combat checks should be suppressed.
   *
   * <p>Always returns {@code true} for players in creative/spectator mode
   * (flight, speed, and similar checks are irrelevant there).
   */
  public boolean isUnderGrace(String playerId) {
    if (creativePlayers.contains(playerId)) return true;
    Long expiry = graceUntilNanos.get(playerId);
    return expiry != null && System.nanoTime() < expiry;
  }

  /**
   * Returns {@code true} specifically for creative/spectator players, independent
   * of grace timeout.  Use this in flight checks where creative is permanently
   * exempt.
   */
  public boolean isCreativeOrSpectator(String playerId) {
    return creativePlayers.contains(playerId);
  }

  /** Clean up all state for a disconnected player. */
  public void onQuit(String playerId) {
    graceUntilNanos.remove(playerId);
    creativePlayers.remove(playerId);
  }

  private void grantGrace(String playerId, long durationMs) {
    long expiryNanos = System.nanoTime() + durationMs * 1_000_000L;
    // Always extend (never shorten) existing grace
    graceUntilNanos.merge(playerId, expiryNanos, Math::max);
  }
}
