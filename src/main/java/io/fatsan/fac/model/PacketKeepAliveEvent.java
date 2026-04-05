package io.fatsan.fac.model;

/**
 * KeepAlive packet (19)
 */
public record PacketKeepAliveEvent(
    String playerId,
    long nanoTime,
    long id,
    boolean clientResponse
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
