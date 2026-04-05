package io.fatsan.fac.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CheckConfigLoader {

  private CheckConfigLoader() {}

  public static CheckSettings parseCheckSettings(Map<String, Object> section) {
    CheckSettings d = CheckSettings.defaults();
    boolean enabled = getBoolean(section, "enabled", d.enabled());
    int bufferLimit = getInt(section, "buffer-limit", d.bufferLimit());
    double threshold = getDouble(section, "threshold", d.threshold());
    int minVl = getInt(section, "min-vl", d.minVl());
    double severityCap = getDouble(section, "severity-cap", d.severityCap());
    Set<String> worlds = getStringSet(section, "worlds");
    Set<String> exemptPerms = getStringSet(section, "exempt-permissions");
    return new CheckSettings(enabled, bufferLimit, threshold, minVl, severityCap, worlds, exemptPerms);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, CheckSettings> parseAllCheckSettings(Map<String, Object> checkSettingsSection) {
    if (checkSettingsSection == null || checkSettingsSection.isEmpty()) {
      return Map.of();
    }
    Map<String, CheckSettings> result = new HashMap<>();
    for (var entry : checkSettingsSection.entrySet()) {
      if (entry.getValue() instanceof Map<?, ?> map) {
        result.put(entry.getKey(), parseCheckSettings((Map<String, Object>) map));
      }
    }
    return Collections.unmodifiableMap(result);
  }

  private static boolean getBoolean(Map<String, Object> m, String key, boolean def) {
    Object v = m.get(key);
    return v instanceof Boolean b ? b : def;
  }

  private static int getInt(Map<String, Object> m, String key, int def) {
    Object v = m.get(key);
    return v instanceof Number n ? n.intValue() : def;
  }

  private static double getDouble(Map<String, Object> m, String key, double def) {
    Object v = m.get(key);
    return v instanceof Number n ? n.doubleValue() : def;
  }

  @SuppressWarnings("unchecked")
  private static Set<String> getStringSet(Map<String, Object> m, String key) {
    Object v = m.get(key);
    if (v instanceof List<?> list) {
      return Set.copyOf(list.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .toList());
    }
    return Set.of();
  }
}
