package io.fatsan.fac.model;

/**
 * Held item change packet (12)
 */
public record PacketHeldItemEvent(
    String playerId,
    long nanoTime,
    int slot
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
