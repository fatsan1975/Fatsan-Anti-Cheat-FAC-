package io.fatsan.fac.service;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maintains a whitelist of known item/entity attributes so that movement and
 * combat checks can recognise custom-plugin attributes without triggering
 * false positives.
 *
 * Any attribute NOT in the whitelist is treated as "unknown" — the calling
 * check should switch to conservative mode (widen thresholds or skip).
 */
public final class CustomAttributeRegistry {

  private static final Set<String> DEFAULT_SPEED = Set.of(
      "generic.movement_speed", "generic.flying_speed", "generic.step_height",
      "generic.safe_fall_distance", "generic.fall_damage_multiplier",
      "generic.jump_strength", "generic.gravity");

  private static final Set<String> DEFAULT_ATTACK = Set.of(
      "generic.attack_speed", "generic.attack_damage", "generic.attack_knockback",
      "player.entity_interaction_range", "player.block_interaction_range",
      "generic.knockback_resistance");

  private final Set<String> knownSpeedAttributes;
  private final Set<String> knownAttackAttributes;
  private final Set<String> ignoredItems;

  public CustomAttributeRegistry(Set<String> speedAttributes, Set<String> attackAttributes, Set<String> ignoredItems) {
    this.knownSpeedAttributes = toLowerCase(speedAttributes);
    this.knownAttackAttributes = toLowerCase(attackAttributes);
    this.ignoredItems = toUpperCase(ignoredItems);
  }

  public static CustomAttributeRegistry withDefaults() {
    return new CustomAttributeRegistry(DEFAULT_SPEED, DEFAULT_ATTACK, Set.of());
  }

  public boolean isKnownSpeedAttribute(String attribute) {
    return knownSpeedAttributes.contains(attribute.toLowerCase(Locale.ROOT));
  }

  public boolean isKnownAttackAttribute(String attribute) {
    return knownAttackAttributes.contains(attribute.toLowerCase(Locale.ROOT));
  }

  public boolean shouldBeConservative(String attribute) {
    String lower = attribute.toLowerCase(Locale.ROOT);
    return !knownSpeedAttributes.contains(lower) && !knownAttackAttributes.contains(lower);
  }

  public boolean isIgnoredItem(String materialName) {
    return ignoredItems.contains(materialName.toUpperCase(Locale.ROOT));
  }

  private static Set<String> toLowerCase(Set<String> set) {
    return set.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
  }

  private static Set<String> toUpperCase(Set<String> set) {
    return set.stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
  }
}
