package io.fatsan.fac.model;

import com.github.retrooper.packetevents.util.Vector3f;

/**
 * Entity interaction packet (6-7)
 */
public record PacketInteractEvent(
    String playerId,
    long nanoTime,
    int entityId,
    InteractType type,
    Vector3f target
) implements PacketEvent {

  public enum InteractType {
    ATTACK,
    INTERACT,
    INTERACT_AT
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
