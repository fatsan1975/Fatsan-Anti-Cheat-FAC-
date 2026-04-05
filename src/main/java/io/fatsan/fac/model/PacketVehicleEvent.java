package io.fatsan.fac.model;

import com.github.retrooper.packetevents.protocol.world.Location;

/**
 * Vehicle packet (22-24)
 *
 * <p>Covers three vehicle packet types with a single record:
 * <ul>
 *   <li>MOVE — vehicle position update (position, yaw, pitch)</li>
 *   <li>STEER — steer vehicle (forward, sideways)</li>
 *   <li>BOAT_STEER — steer boat (leftPaddle, rightPaddle)</li>
 * </ul>
 * Unused fields for a given action type are 0/false.
 */
public record PacketVehicleEvent(
    String playerId,
    long nanoTime,
    Location position,
    float yaw,
    float pitch,
    VehicleAction action,
    float forward,
    float sideways,
    boolean leftPaddle,
    boolean rightPaddle
) implements PacketEvent {

  /** Factory for STEER_VEHICLE packets. */
  public static PacketVehicleEvent steer(String playerId, long nanoTime, float forward, float sideways) {
    return new PacketVehicleEvent(playerId, nanoTime, null, 0f, 0f,
        VehicleAction.STEER, forward, sideways, false, false);
  }

  /** Factory for STEER_BOAT packets. */
  public static PacketVehicleEvent boatSteer(String playerId, long nanoTime,
      Location position, float yaw, float pitch,
      boolean leftPaddle, boolean rightPaddle) {
    return new PacketVehicleEvent(playerId, nanoTime, position, yaw, pitch,
        VehicleAction.BOAT_STEER, 0f, 0f, leftPaddle, rightPaddle);
  }

  /** Factory for VEHICLE_MOVE packets. */
  public static PacketVehicleEvent move(String playerId, long nanoTime,
      Location position, float yaw, float pitch) {
    return new PacketVehicleEvent(playerId, nanoTime, position, yaw, pitch,
        VehicleAction.MOVE, 0f, 0f, false, false);
  }

  public enum VehicleAction {
    MOVE,
    STEER,
    BOAT_STEER
  }

  @Override
  public long serverTick() {
    return -1L;
  }

  @Override
  public long regionTick() {
    return -1L;
  }
}
