package io.fatsan.fac.model;

/**
 * Entity action packet (16-18)
 */
public record PacketEntityActionEvent(
    String playerId,
    long nanoTime,
    ActionType action,
    int jumpBoost
) implements PacketEvent {

  public enum ActionType {
    START_SPRINT,
    STOP_SPRINT,
    START_SNEAK,
    STOP_SNEAK,
    HORSE_JUMP,
    HORSE_JUMP_STOP,
    HORSE_INVENTORY,
    ELYTRA_START,
    JUMP,
    HORSE_JUMP_BOOSTED,
    UNKNOWN
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
