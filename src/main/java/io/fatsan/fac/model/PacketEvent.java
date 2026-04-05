package io.fatsan.fac.model;

/**
 * Packet-level event marker interface
 * 2. Packet abstraction layer
 */
public sealed interface PacketEvent extends NormalizedEvent
    permits PacketFlyingEvent,
        PacketInteractEvent,
        PacketDiggingEvent,
        PacketPlaceEvent,
        PacketHeldItemEvent,
        PacketWindowEvent,
        PacketEntityActionEvent,
        PacketKeepAliveEvent,
        PacketTransactionEvent,
        PacketVehicleEvent {

  default String packetType() {
    return getClass().getSimpleName();
  }
}
