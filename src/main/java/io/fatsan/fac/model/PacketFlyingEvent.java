package io.fatsan.fac.model;

/**
 * Flying packet abstraction (3-5)
 */
public record PacketFlyingEvent(
    String playerId,
    long nanoTime,
    double x, double y, double z,
    float yaw, float pitch,
    boolean onGround,
    FlyingType type
) implements PacketEvent {

  public enum FlyingType {
    FLYING,           // Just ground state
    POSITION,         // Position only
    ROTATION,         // Rotation only
    POSITION_AND_ROTATION
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
