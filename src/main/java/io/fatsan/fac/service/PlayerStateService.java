package io.fatsan.fac.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;

public final class PlayerStateService {
  private final Map<UUID, Location> lastSafeLocation = new ConcurrentHashMap<>();

  public void updateSafeLocation(UUID uuid, Location location) {
    lastSafeLocation.put(uuid, location.clone());
  }

  public Location getSafeLocation(UUID uuid) {
    Location location = lastSafeLocation.get(uuid);
    return location == null ? null : location.clone();
  }

  public void clear(UUID uuid) {
    lastSafeLocation.remove(uuid);
  }
}
