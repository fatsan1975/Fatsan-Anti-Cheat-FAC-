package io.fatsan.fac.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CheckConfigLoaderTest {

    @Test
    void parseCheckSettingsFromMap() {
        Map<String, Object> checkSection = Map.of(
            "enabled", false,
            "threshold", 15.0,
            "buffer-limit", 10,
            "min-vl", 8,
            "severity-cap", 0.5,
            "worlds", java.util.List.of("pvp_arena"),
            "exempt-permissions", java.util.List.of("vip.bypass")
        );
        CheckSettings settings = CheckConfigLoader.parseCheckSettings(checkSection);
        assertFalse(settings.enabled());
        assertEquals(15.0, settings.threshold());
        assertEquals(10, settings.bufferLimit());
        assertEquals(8, settings.minVl());
        assertEquals(0.5, settings.severityCap());
        assertEquals(Set.of("pvp_arena"), settings.worlds());
        assertEquals(Set.of("vip.bypass"), settings.exemptPermissions());
    }

    @Test
    void parsePartialConfigUsesDefaults() {
        Map<String, Object> checkSection = Map.of("threshold", 20.0);
        CheckSettings settings = CheckConfigLoader.parseCheckSettings(checkSection);
        assertTrue(settings.enabled());
        assertEquals(20.0, settings.threshold());
        assertEquals(6, settings.bufferLimit());
    }

    @Test
    void parseEmptyMapReturnsDefaults() {
        CheckSettings settings = CheckConfigLoader.parseCheckSettings(Map.of());
        assertTrue(settings.enabled());
        assertEquals(0.0, settings.threshold());
        assertEquals(6, settings.bufferLimit());
    }
}
