package io.fatsan.fac.service;

import io.fatsan.fac.model.ProtocolProfile;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class ProtocolProfileResolver {
  private static final long CACHE_TTL_NANOS = 5_000_000_000L;
  private static final ViaHandle VIA_HANDLE = ViaHandle.tryCreate();
  private static final boolean PACKET_EVENTS_AVAILABLE = isPacketEventsAvailable();

  private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

  /** Removes the cached protocol profile for the given player. Called on disconnect. */
  public void clearPlayer(UUID playerId) {
    cache.remove(playerId);
  }

  public ProtocolProfile resolve(Player player) {
    UUID uuid = player.getUniqueId();
    long now = System.nanoTime();
    CacheEntry cached = cache.get(uuid);
    if (cached != null && now - cached.nanoTime < CACHE_TTL_NANOS) {
      return cached.profile;
    }

    Integer version = VIA_HANDLE.readVersion(uuid);
    ProtocolProfile profile = mapVersion(version);
    if (!PACKET_EVENTS_AVAILABLE && profile == ProtocolProfile.UNKNOWN) {
      profile = ProtocolProfile.LATEST_1_21;
    }
    cache.put(uuid, new CacheEntry(now, profile));
    return profile;
  }

  private static ProtocolProfile mapVersion(Integer version) {
    if (version == null) return ProtocolProfile.UNKNOWN;
    if (version <= 47) return ProtocolProfile.LEGACY_1_8;
    if (version <= 340) return ProtocolProfile.LEGACY_1_9_1_12;
    if (version <= 578) return ProtocolProfile.LEGACY_1_13_1_15;
    if (version <= 735) return ProtocolProfile.MODERN_1_16;
    if (version <= 756) return ProtocolProfile.MODERN_1_17;
    if (version <= 758) return ProtocolProfile.MODERN_1_18;
    if (version <= 762) return ProtocolProfile.MODERN_1_19;
    if (version <= 766) return ProtocolProfile.MODERN_1_20;
    return ProtocolProfile.LATEST_1_21;
  }

  private static boolean isPacketEventsAvailable() {
    try {
      Class.forName("com.github.retrooper.packetevents.PacketEvents");
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }

  private record CacheEntry(long nanoTime, ProtocolProfile profile) {}

  private static final class ViaHandle {
    private static final ViaHandle NOOP = new ViaHandle(null, null);

    private final Object api;
    private final Method getPlayerVersion;

    private ViaHandle(Object api, Method getPlayerVersion) {
      this.api = api;
      this.getPlayerVersion = getPlayerVersion;
    }

    private static ViaHandle tryCreate() {
      try {
        Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");
        Method getAPI = viaClass.getMethod("getAPI");
        Object api = getAPI.invoke(null);
        Method getPlayerVersion = api.getClass().getMethod("getPlayerVersion", UUID.class);
        return new ViaHandle(api, getPlayerVersion);
      } catch (Throwable ignored) {
        return NOOP;
      }
    }

    private Integer readVersion(UUID uuid) {
      if (api == null || getPlayerVersion == null) {
        return null;
      }
      try {
        Object value = getPlayerVersion.invoke(api, uuid);
        return value instanceof Integer integer ? integer : null;
      } catch (Throwable ignored) {
        return null;
      }
    }
  }
}
