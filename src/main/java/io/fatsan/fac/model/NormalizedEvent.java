package io.fatsan.fac.model;

public sealed interface NormalizedEvent
    permits MovementEvent,
        RotationEvent,
        CombatHitEvent,
        BlockPlaceEventSignal,
        BlockBreakEventSignal,
        InventoryClickEventSignal,
        KeepAliveSignal,
        TeleportSignal,
        TrafficSignal,
        PlayerStateEvent,
        PacketEvent,
        CombatContextSignal,
        MovementContextSignal,
        BlockBreakContextSignal,
        FishingSignal,
        HotbarSignal {
  String playerId();

  long nanoTime();

  default int schemaVersion() {
    return 2;
  }

  default String sourceAdapterVersion() {
    return "fac-bridge-2";
  }

  default long serverTick() {
    return -1L;
  }

  default long regionTick() {
    return -1L;
  }

  default String traceId() {
    return playerId() + ":" + nanoTime();
  }
}
