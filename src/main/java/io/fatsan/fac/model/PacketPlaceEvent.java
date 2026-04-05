package io.fatsan.fac.model;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;

/**
 * Block place packet (8)
 */
public record PacketPlaceEvent(
    String playerId,
    long nanoTime,
    Vector3i blockPosition,
    BlockFace direction,
    Vector3f cursorPosition
) implements PacketEvent {

  @Override
  public long serverTick() {
    return -1L;
  }

  @Override
  public long regionTick() {
    return -1L;
  }
}
