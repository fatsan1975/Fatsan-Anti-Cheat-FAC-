package io.fatsan.fac.config;

import java.util.Map;

/**
 * Per-world anti-cheat configuration. Allows disabling FAC entirely in
 * specific worlds (lobbies) and overriding the policy profile per world.
 */
public final class WorldConfigService {
  private final Map<String, WorldSettings> worldSettings;

  public WorldConfigService(Map<String, WorldSettings> worldSettings) {
    this.worldSettings = Map.copyOf(worldSettings);
  }

  public boolean isActive(String worldName) {
    WorldSettings settings = resolve(worldName);
    return settings == null || settings.enabled();
  }

  public String policyProfile(String worldName) {
    WorldSettings settings = resolve(worldName);
    return settings != null ? settings.policyProfile() : "default";
  }

  private WorldSettings resolve(String worldName) {
    WorldSettings specific = worldSettings.get(worldName);
    if (specific != null) return specific;
    return worldSettings.get("*");
  }

  public record WorldSettings(boolean enabled, String policyProfile) {}
}
