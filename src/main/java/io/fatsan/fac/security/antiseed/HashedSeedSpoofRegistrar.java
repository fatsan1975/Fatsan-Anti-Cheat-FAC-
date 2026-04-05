package io.fatsan.fac.security.antiseed;

import com.github.retrooper.packetevents.PacketEvents;

/**
 * Isolated helper that touches PacketEvents classes. Loaded only when
 * PacketEvents is confirmed present at runtime, preventing
 * NoClassDefFoundError in environments without PacketEvents.
 */
final class HashedSeedSpoofRegistrar {

  private HashedSeedSpoofRegistrar() {}

  static void register() {
    PacketEvents.getAPI().getEventManager().registerListener(new HashedSeedSpoofListener());
  }
}
