package io.fatsan.fac.config;

import java.util.Map;

/**
 * Central registry of per-check configuration. Falls back to sensible defaults
 * for any check that is not explicitly configured.
 *
 * Immutable after construction — a new instance is created on /fac reload.
 */
public final class CheckConfigService {
  private final Map<String, CheckSettings> settingsByCheck;

  public CheckConfigService(Map<String, CheckSettings> settingsByCheck) {
    this.settingsByCheck = Map.copyOf(settingsByCheck);
  }

  /** Returns settings for the given check, or defaults if not configured. */
  public CheckSettings get(String checkName) {
    return settingsByCheck.getOrDefault(checkName, CheckSettings.defaults());
  }

  /** Shorthand: is this check enabled in its config? */
  public boolean isEnabled(String checkName) {
    return get(checkName).enabled();
  }

  /** Returns true if the check should run in the given world. */
  public boolean isActiveInWorld(String checkName, String worldName) {
    return get(checkName).activeInWorld(worldName);
  }
}
