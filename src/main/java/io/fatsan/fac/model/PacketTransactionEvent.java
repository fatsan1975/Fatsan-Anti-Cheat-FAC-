package io.fatsan.fac.model;

/**
 * Transaction packet (20-21)
 */
public record PacketTransactionEvent(
    String playerId,
    long nanoTime,
    int windowId,
    int actionId,
    boolean accepted,
    TransactionDirection direction
) implements PacketEvent {

  public enum TransactionDirection {
    CLIENT_TO_SERVER,
    SERVER_TO_CLIENT
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
