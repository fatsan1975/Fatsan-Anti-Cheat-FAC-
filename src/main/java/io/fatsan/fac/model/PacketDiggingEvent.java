package io.fatsan.fac.model;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;

/**
 * Block digging packet (9-11)
 */
public record PacketDiggingEvent(
    String playerId,
    long nanoTime,
    Vector3i blockPosition,
    DiggingAction action,
    BlockFace direction
) implements PacketEvent {

  public enum DiggingAction {
    START,
    CANCEL,
    FINISH,
    DROP_STACK,
    DROP_ITEM,
    RELEASE_ITEM,
    SWAP_OFFHAND,
    STAB
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
