package io.fatsan.fac.service;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CustomAttributeRegistryTest {

    @Test
    void defaultRegistryRecognizesVanillaAttributes() {
        var registry = CustomAttributeRegistry.withDefaults();
        assertTrue(registry.isKnownSpeedAttribute("generic.movement_speed"));
        assertTrue(registry.isKnownAttackAttribute("generic.attack_speed"));
        assertTrue(registry.isKnownAttackAttribute("generic.attack_damage"));
    }

    @Test
    void customSpeedAttributeIsRecognized() {
        var registry = new CustomAttributeRegistry(
            Set.of("generic.movement_speed", "myplugin.speed_boost"),
            Set.of("generic.attack_speed"), Set.of());
        assertTrue(registry.isKnownSpeedAttribute("myplugin.speed_boost"));
        assertFalse(registry.isKnownSpeedAttribute("unknown.attr"));
    }

    @Test
    void unknownAttributeTriggersConservativeMode() {
        var registry = CustomAttributeRegistry.withDefaults();
        assertFalse(registry.isKnownSpeedAttribute("modded.super_speed"));
        assertTrue(registry.shouldBeConservative("modded.super_speed"));
    }

    @Test
    void ignoredItemsAreRecognized() {
        var registry = new CustomAttributeRegistry(Set.of(), Set.of(), Set.of("COMMAND_BLOCK", "BARRIER"));
        assertTrue(registry.isIgnoredItem("COMMAND_BLOCK"));
        assertFalse(registry.isIgnoredItem("DIAMOND_SWORD"));
    }

    @Test
    void caseInsensitiveMatching() {
        var registry = new CustomAttributeRegistry(Set.of("generic.movement_speed"), Set.of(), Set.of());
        assertTrue(registry.isKnownSpeedAttribute("GENERIC.MOVEMENT_SPEED"));
        assertTrue(registry.isKnownSpeedAttribute("Generic.Movement_Speed"));
    }
}
