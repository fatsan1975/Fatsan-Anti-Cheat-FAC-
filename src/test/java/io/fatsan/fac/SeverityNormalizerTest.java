package io.fatsan.fac;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.service.SeverityNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeverityNormalizerTest {

  // ── Core punish candidates — full cap 1.0 ──────────────────────────────

  @Test
  void corePunishCandidateAllowsFullSeverity() {
    double result = SeverityNormalizer.normalize("SpeedEnvelope", CheckCategory.MOVEMENT, 0.95);
    assertEquals(0.95, result, 0.001);
  }

  @Test
  void corePunishCandidateCapAtOne() {
    double result = SeverityNormalizer.normalize("NoFallHeuristic", CheckCategory.MOVEMENT, 1.5);
    assertEquals(1.0, result, 0.001);
  }

  @Test
  void reachHeuristicIsCorePunish() {
    double result = SeverityNormalizer.normalize("ReachHeuristic", CheckCategory.COMBAT, 0.8);
    assertEquals(0.8, result, 0.001);
  }

  @Test
  void scaffoldPatternIsCorePunish() {
    double result = SeverityNormalizer.normalize("ScaffoldPattern", CheckCategory.WORLD, 0.75);
    assertEquals(0.75, result, 0.001);
  }

  @Test
  void fastBreakIsCorePunish() {
    double result = SeverityNormalizer.normalize("FastBreak", CheckCategory.WORLD, 0.9);
    assertEquals(0.9, result, 0.001);
  }

  // ── Deep item context — capped at 0.45 ───────────────────────────────

  @Test
  void attributeCheckCappedAt045() {
    double result = SeverityNormalizer.normalize("BlockBreakAttributeOutlier", CheckCategory.WORLD, 0.9);
    assertEquals(0.45, result, 0.001);
  }

  @Test
  void enchantCheckCappedAt045() {
    double result = SeverityNormalizer.normalize("BlockBreakEnchantmentWeight", CheckCategory.WORLD, 1.0);
    assertEquals(0.45, result, 0.001);
  }

  @Test
  void metaCheckCappedAt045() {
    double result = SeverityNormalizer.normalize("BlockBreakMetaSignatureDrift", CheckCategory.WORLD, 0.8);
    assertEquals(0.45, result, 0.001);
  }

  // ── Via-derived — capped at 0.55 ─────────────────────────────────────

  @Test
  void viaCheckCappedAt055() {
    double result = SeverityNormalizer.normalize("CombatViaWindowSmear", CheckCategory.COMBAT, 0.9);
    assertEquals(0.55, result, 0.001);
  }

  @Test
  void viaTransactionSkewCappedAt055() {
    double result = SeverityNormalizer.normalize("InventoryViaTransactionSkew", CheckCategory.INVENTORY, 1.0);
    assertEquals(0.55, result, 0.001);
  }

  // ── Timing-derived — capped at 0.55 ──────────────────────────────────

  @Test
  void timingMismatchCappedAt055() {
    double result = SeverityNormalizer.normalize("CombatProtocolTimingMismatch", CheckCategory.COMBAT, 0.9);
    assertEquals(0.55, result, 0.001);
  }

  @Test
  void bundleOrderCappedAt055() {
    double result = SeverityNormalizer.normalize("InventoryPacketBundleDesync", CheckCategory.INVENTORY, 0.8);
    assertEquals(0.55, result, 0.001);
  }

  // ── Statistical families — capped at 0.65 ────────────────────────────

  @Test
  void entropyCheckCappedAt065() {
    double result = SeverityNormalizer.normalize("CombatIntervalEntropy", CheckCategory.COMBAT, 0.9);
    assertEquals(0.65, result, 0.001);
  }

  @Test
  void varianceCollapseCheckCappedAt065() {
    double result = SeverityNormalizer.normalize("ReachVarianceCollapse", CheckCategory.COMBAT, 1.0);
    assertEquals(0.65, result, 0.001);
  }

  @Test
  void cadenceLockCappedAt065() {
    double result = SeverityNormalizer.normalize("InventoryCadenceLock", CheckCategory.INVENTORY, 0.8);
    assertEquals(0.65, result, 0.001);
  }

  @Test
  void plateauCheckCappedAt065() {
    double result = SeverityNormalizer.normalize("CombatHitIntervalPlateau", CheckCategory.COMBAT, 0.9);
    assertEquals(0.65, result, 0.001);
  }

  // ── Protocol noise — capped at 0.70 ──────────────────────────────────

  @Test
  void keepAliveCheckCappedAt070() {
    double result = SeverityNormalizer.normalize("KeepAliveDrift", CheckCategory.PROTOCOL, 0.9);
    assertEquals(0.70, result, 0.001);
  }

  @Test
  void pingCheckCappedAt070() {
    double result = SeverityNormalizer.normalize("PingSpoofHeuristic", CheckCategory.PROTOCOL, 1.0);
    assertEquals(0.70, result, 0.001);
  }

  @Test
  void trafficCheckCappedAt070() {
    double result = SeverityNormalizer.normalize("TrafficFloodRamp", CheckCategory.PROTOCOL, 0.9);
    assertEquals(0.70, result, 0.001);
  }

  // ── Category fallback caps ────────────────────────────────────────────

  @Test
  void movementFallbackCappedAt090() {
    double result = SeverityNormalizer.normalize("MovementCadence", CheckCategory.MOVEMENT, 0.95);
    // cadence → statistical family → 0.65 (takes priority over category fallback)
    assertEquals(0.65, result, 0.001);
  }

  @Test
  void inventoryFallbackCappedAt070() {
    double result = SeverityNormalizer.normalize("InventoryMove", CheckCategory.INVENTORY, 0.9);
    assertEquals(0.70, result, 0.001);
  }

  // ── Edge cases ────────────────────────────────────────────────────────

  @Test
  void zeroSeverityAlwaysZero() {
    double result = SeverityNormalizer.normalize("SpeedEnvelope", CheckCategory.MOVEMENT, 0.0);
    assertEquals(0.0, result, 0.001);
  }

  @Test
  void negativeSeverityClampedToZero() {
    double result = SeverityNormalizer.normalize("SpeedEnvelope", CheckCategory.MOVEMENT, -0.5);
    assertEquals(0.0, result, 0.001);
  }

  @Test
  void caseInsensitiveMatching() {
    double lower = SeverityNormalizer.normalize("speedenvelope", CheckCategory.MOVEMENT, 0.8);
    double upper = SeverityNormalizer.normalize("SPEEDENVELOPE", CheckCategory.MOVEMENT, 0.8);
    assertEquals(lower, upper, 0.001);
  }
}
