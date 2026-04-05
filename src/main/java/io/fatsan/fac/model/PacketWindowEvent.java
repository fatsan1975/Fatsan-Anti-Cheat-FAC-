package io.fatsan.fac.model;

import com.github.retrooper.packetevents.protocol.item.ItemStack;

/**
 * Window/Inventory packet (13-15)
 */
public record PacketWindowEvent(
    String playerId,
    long nanoTime,
    int windowId,
    WindowAction action,
    int slot,
    int button,
    int actionNumber,
    ItemStack itemStack
) implements PacketEvent {

  public PacketWindowEvent(String playerId, long nanoTime, int windowId, WindowAction action) {
    this(playerId, nanoTime, windowId, action, -1, -1, -1, null);
  }

  public PacketWindowEvent(String playerId, long nanoTime, int windowId, WindowAction action,
                           int slot, int button, int actionNumber) {
    this(playerId, nanoTime, windowId, action, slot, button, actionNumber, null);
  }

  public enum WindowAction {
    CLOSE,
    CLICK,
    CREATIVE_ACTION
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
