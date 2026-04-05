package io.fatsan.fac.config;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CheckConfigServiceTest {

    @Test
    void defaultSettingsArePermissive() {
        var settings = CheckSettings.defaults();
        assertTrue(settings.enabled());
        assertEquals(6, settings.bufferLimit());
        assertEquals(1.0, settings.severityCap());
        assertTrue(settings.worlds().isEmpty());
        assertTrue(settings.exemptPermissions().isEmpty());
    }

    @Test
    void customSettingsOverrideDefaults() {
        var settings = new CheckSettings(
            false, 10, 8.5, 4, 0.8,
            Set.of("lobby", "hub"),
            Set.of("myplugin.bypass"));
        assertFalse(settings.enabled());
        assertEquals(10, settings.bufferLimit());
        assertEquals(8.5, settings.threshold());
        assertEquals(4, settings.minVl());
        assertEquals(0.8, settings.severityCap());
        assertEquals(Set.of("lobby", "hub"), settings.worlds());
        assertEquals(Set.of("myplugin.bypass"), settings.exemptPermissions());
    }

    @Test
    void serviceReturnsDefaultsForUnknownCheck() {
        var service = new CheckConfigService(java.util.Map.of());
        var settings = service.get("UnknownCheck");
        assertTrue(settings.enabled());
        assertEquals(6, settings.bufferLimit());
    }

    @Test
    void serviceReturnsConfiguredSettings() {
        var custom = new CheckSettings(false, 12, 15.0, 8, 0.5,
            Set.of("pvp_arena"), Set.of());
        var service = new CheckConfigService(java.util.Map.of("SpeedEnvelope", custom));
        var settings = service.get("SpeedEnvelope");
        assertFalse(settings.enabled());
        assertEquals(12, settings.bufferLimit());
        assertEquals(15.0, settings.threshold());
    }

    @Test
    void isEnabledReturnsFalseForDisabledCheck() {
        var custom = new CheckSettings(false, 6, 11.5, 6, 1.0, Set.of(), Set.of());
        var service = new CheckConfigService(java.util.Map.of("SpeedEnvelope", custom));
        assertFalse(service.isEnabled("SpeedEnvelope"));
        assertTrue(service.isEnabled("UnknownCheck"));
    }

    @Test
    void worldFilterWorksCorrectly() {
        var custom = new CheckSettings(true, 6, 11.5, 6, 1.0,
            Set.of("pvp_arena"), Set.of());
        var service = new CheckConfigService(java.util.Map.of("SpeedEnvelope", custom));
        assertTrue(service.isActiveInWorld("SpeedEnvelope", "pvp_arena"));
        assertFalse(service.isActiveInWorld("SpeedEnvelope", "lobby"));
        // Empty worlds = all worlds allowed
        assertTrue(service.isActiveInWorld("UnknownCheck", "any_world"));
    }
}
