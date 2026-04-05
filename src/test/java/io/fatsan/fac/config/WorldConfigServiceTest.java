package io.fatsan.fac.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WorldConfigServiceTest {
    @Test void disabledWorldReturnsInactive() {
        var service = new WorldConfigService(Map.of(
            "lobby", new WorldConfigService.WorldSettings(false, "default")));
        assertFalse(service.isActive("lobby"));
        assertTrue(service.isActive("survival"));
    }
    @Test void worldOverridesPolicyProfile() {
        var service = new WorldConfigService(Map.of(
            "pvp_arena", new WorldConfigService.WorldSettings(true, "strict")));
        assertEquals("strict", service.policyProfile("pvp_arena"));
        assertEquals("default", service.policyProfile("survival"));
    }
    @Test void wildcardAppliesAsDefault() {
        var service = new WorldConfigService(Map.of(
            "*", new WorldConfigService.WorldSettings(true, "lightweight"),
            "pvp", new WorldConfigService.WorldSettings(true, "strict")));
        assertEquals("strict", service.policyProfile("pvp"));
        assertEquals("lightweight", service.policyProfile("unknown_world"));
    }
    @Test void emptyConfigDefaultsToActive() {
        var service = new WorldConfigService(Map.of());
        assertTrue(service.isActive("any_world"));
        assertEquals("default", service.policyProfile("any_world"));
    }
}
