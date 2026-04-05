package io.fatsan.fac.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared state bridge between PacketEvents Layer 2 and Bukkit Layer 1.
 * When PacketEvents is available, populated with server-side ground truth,
 * validated positions, tick timing, and precise reach.
 * When PE is NOT available, noop() returns safe defaults.
 * Thread-safe: all state in ConcurrentHashMap.
 */
public final class PacketContext {

  private final boolean packetEventsAvailable;
  private final Map<String, Boolean> serverGround;
  private final Map<String, double[]> lastValidPosition;
  private final Map<String, Long> tickBalance;
  private final Map<String, Double> preciseReach;

  private PacketContext(boolean packetEventsAvailable) {
    this.packetEventsAvailable = packetEventsAvailable;
    this.serverGround = new ConcurrentHashMap<>();
    this.lastValidPosition = new ConcurrentHashMap<>();
    this.tickBalance = new ConcurrentHashMap<>();
    this.preciseReach = new ConcurrentHashMap<>();
  }

  public static PacketContext live() { return new PacketContext(true); }
  public static PacketContext noop() { return new PacketContext(false); }

  public boolean isPacketEventsAvailable() { return packetEventsAvailable; }

  public void setServerGround(String playerId, boolean onGround) { serverGround.put(playerId, onGround); }
  public boolean getServerGround(String playerId) {
    if (!packetEventsAvailable) return true;
    return serverGround.getOrDefault(playerId, true);
  }

  public void setLastValidPosition(String playerId, double x, double y, double z) {
    lastValidPosition.put(playerId, new double[]{x, y, z});
  }
  public double getLastValidY(String playerId) {
    double[] pos = lastValidPosition.get(playerId);
    return pos != null ? pos[1] : -1.0;
  }
  public double[] getLastValidPosition(String playerId) {
    return lastValidPosition.get(playerId);
  }

  public void setTickBalance(String playerId, long balance) { tickBalance.put(playerId, balance); }
  public long getTickBalance(String playerId) { return tickBalance.getOrDefault(playerId, -1L); }

  public void setPreciseReach(String playerId, double distance) { preciseReach.put(playerId, distance); }
  public double getPreciseReach(String playerId) { return preciseReach.getOrDefault(playerId, -1.0); }

  public void clearPlayer(String playerId) {
    serverGround.remove(playerId);
    lastValidPosition.remove(playerId);
    tickBalance.remove(playerId);
    preciseReach.remove(playerId);
  }
}
