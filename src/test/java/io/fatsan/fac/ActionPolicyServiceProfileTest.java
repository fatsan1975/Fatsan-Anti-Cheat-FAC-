package io.fatsan.fac;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.service.ActionDisposition;
import io.fatsan.fac.service.ActionPolicyService;
import io.fatsan.fac.service.SuspicionTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ActionPolicyService profile-specific behaviour for the profiles added
 * in Iteration 5: minigame, survival, via-heavy, and the existing strict/
 * lightweight/custom-mechanics-safe profiles for regression coverage.
 */
class ActionPolicyServiceProfileTest {

  // ── minigame ──────────────────────────────────────────────────────────

  @Test
  void minigame_teleportFamilyIsReviewOnly() {
    ActionPolicyService svc = new ActionPolicyService("minigame");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("TeleportChain", CheckCategory.PROTOCOL);
    assertEquals(ActionDisposition.REVIEW_ONLY, p.disposition());
    assertEquals("minigame-teleport", p.family());
  }

  @Test
  void minigame_scaffoldIsAlertOnly() {
    ActionPolicyService svc = new ActionPolicyService("minigame");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("ScaffoldPattern", CheckCategory.WORLD);
    assertEquals(ActionDisposition.ALERT_ONLY, p.disposition());
    assertEquals("minigame-build-world", p.family());
  }

  @Test
  void minigame_reachHeuristicIsCorroboratedSetback() {
    ActionPolicyService svc = new ActionPolicyService("minigame");
    // Reach is a core punish candidate — not in build/world family
    ActionPolicyService.ResolvedPolicy p = svc.resolve("ReachHeuristic", CheckCategory.COMBAT);
    assertEquals(ActionDisposition.CORROBORATED_SETBACK, p.disposition());
    assertEquals("core-punish", p.family());
  }

  @Test
  void minigame_statisticalFamilyIsHotTier() {
    ActionPolicyService svc = new ActionPolicyService("minigame");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("CombatIntervalEntropy", CheckCategory.COMBAT);
    assertEquals(ActionDisposition.ALERT_ONLY, p.disposition());
    assertEquals(SuspicionTier.HOT, p.minimumEvaluationTier());
  }

  // ── via-heavy ────────────────────────────────────────────────────────

  @Test
  void viaHeavy_viaFamilyIsReviewOnlyAtHotTier() {
    ActionPolicyService svc = new ActionPolicyService("via-heavy");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("CombatViaWindowSmear", CheckCategory.COMBAT);
    assertEquals(ActionDisposition.REVIEW_ONLY, p.disposition());
    assertEquals(SuspicionTier.HOT, p.minimumEvaluationTier());
  }

  @Test
  void viaHeavy_nonViaFamilyBehavesNormally() {
    ActionPolicyService svc = new ActionPolicyService("via-heavy");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("SpeedEnvelope", CheckCategory.MOVEMENT);
    assertEquals(ActionDisposition.CORROBORATED_SETBACK, p.disposition());
    assertEquals("core-punish", p.family());
  }

  @Test
  void viaHeavy_timingDerivedIsReviewOnlyHot() {
    ActionPolicyService svc = new ActionPolicyService("via-heavy");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("CombatProtocolTimingMismatch", CheckCategory.COMBAT);
    assertEquals(ActionDisposition.REVIEW_ONLY, p.disposition());
    assertEquals(SuspicionTier.HOT, p.minimumEvaluationTier());
  }

  // ── strict ───────────────────────────────────────────────────────────

  @Test
  void strict_coreIsCorroboratedKick() {
    ActionPolicyService svc = new ActionPolicyService("strict");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("NoFallHeuristic", CheckCategory.MOVEMENT);
    assertEquals(ActionDisposition.CORROBORATED_KICK, p.disposition());
  }

  @Test
  void strict_viaDerivedIsReviewElevated() {
    ActionPolicyService svc = new ActionPolicyService("strict");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("InventoryViaTransactionSkew", CheckCategory.INVENTORY);
    assertEquals(ActionDisposition.REVIEW_ONLY, p.disposition());
    assertEquals(SuspicionTier.ELEVATED, p.minimumEvaluationTier());
  }

  @Test
  void strict_statisticalFamilyIsElevatedTier() {
    ActionPolicyService svc = new ActionPolicyService("strict");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("ReachVarianceCollapse", CheckCategory.COMBAT);
    assertEquals(ActionDisposition.ALERT_ONLY, p.disposition());
    assertEquals(SuspicionTier.ELEVATED, p.minimumEvaluationTier());
  }

  // ── lightweight ──────────────────────────────────────────────────────

  @Test
  void lightweight_protocolNoiseIsHotTier() {
    ActionPolicyService svc = new ActionPolicyService("lightweight");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("KeepAliveDrift", CheckCategory.PROTOCOL);
    assertEquals(ActionDisposition.ALERT_ONLY, p.disposition());
    assertEquals(SuspicionTier.HOT, p.minimumEvaluationTier());
  }

  // ── custom-mechanics-safe ────────────────────────────────────────────

  @Test
  void customMechanicsSafe_deepItemIsDisabled() {
    ActionPolicyService svc = new ActionPolicyService("custom-mechanics-safe");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("BlockBreakAttributeOutlier", CheckCategory.WORLD);
    assertEquals(ActionDisposition.DISABLED_BY_DEFAULT, p.disposition());
  }

  // ── default ──────────────────────────────────────────────────────────

  @Test
  void defaultProfile_deepItemIsReviewOnly() {
    ActionPolicyService svc = new ActionPolicyService("default");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("BlockBreakEnchantmentWeight", CheckCategory.WORLD);
    assertEquals(ActionDisposition.REVIEW_ONLY, p.disposition());
  }

  @Test
  void defaultProfile_coreIsCorroboratedSetback() {
    ActionPolicyService svc = new ActionPolicyService("default");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("ImpossibleCritical", CheckCategory.COMBAT);
    assertEquals(ActionDisposition.CORROBORATED_SETBACK, p.disposition());
    assertEquals(SuspicionTier.BASELINE, p.minimumEvaluationTier());
  }

  // ── survival ──────────────────────────────────────────────────────────

  @Test
  void survival_buildWorldStatFamilyIsElevatedTier() {
    ActionPolicyService svc = new ActionPolicyService("survival");
    // In survival, block-break statistical families should trigger at ELEVATED,
    // not HOT — nukers/speedmines are more impactful in survival servers.
    ActionPolicyService.ResolvedPolicy p = svc.resolve("BreakIntervalVarianceCollapse", CheckCategory.WORLD);
    assertEquals(ActionDisposition.ALERT_ONLY, p.disposition());
    assertEquals(SuspicionTier.ELEVATED, p.minimumEvaluationTier());
    assertEquals("survival-build-stat", p.family());
  }

  @Test
  void survival_blockPlaceStatFamilyIsElevatedTier() {
    ActionPolicyService svc = new ActionPolicyService("survival");
    ActionPolicyService.ResolvedPolicy p = svc.resolve("BlockPlaceCadenceEntropy", CheckCategory.WORLD);
    assertEquals(SuspicionTier.ELEVATED, p.minimumEvaluationTier());
  }

  @Test
  void survival_combatStatFamilyRemainsHotTier() {
    ActionPolicyService svc = new ActionPolicyService("survival");
    // Non-build-world statistical families stay at HOT tier in survival
    ActionPolicyService.ResolvedPolicy p = svc.resolve("CombatIntervalEntropy", CheckCategory.COMBAT);
    assertEquals(ActionDisposition.ALERT_ONLY, p.disposition());
    assertEquals(SuspicionTier.HOT, p.minimumEvaluationTier());
  }

  @Test
  void survival_corePunishIsCorroboratedSetback() {
    ActionPolicyService svc = new ActionPolicyService("survival");
    // Core punish candidates should be same as default
    ActionPolicyService.ResolvedPolicy p = svc.resolve("SpeedEnvelope", CheckCategory.MOVEMENT);
    assertEquals(ActionDisposition.CORROBORATED_SETBACK, p.disposition());
    assertEquals("core-punish", p.family());
  }

  // ── shouldEvaluate ───────────────────────────────────────────────────

  @Test
  void shouldEvaluate_disabledByDefaultNeverEvaluated() {
    ActionPolicyService svc = new ActionPolicyService("custom-mechanics-safe");
    assertFalse(svc.shouldEvaluate("BlockBreakAttributeOutlier", CheckCategory.WORLD, SuspicionTier.HOT));
  }

  @Test
  void shouldEvaluate_hotFamilySkippedAtBaseline() {
    ActionPolicyService svc = new ActionPolicyService("default");
    // statistical families require ELEVATED or HOT tier
    assertFalse(svc.shouldEvaluate("CombatIntervalEntropy", CheckCategory.COMBAT, SuspicionTier.BASELINE));
    assertTrue(svc.shouldEvaluate("CombatIntervalEntropy", CheckCategory.COMBAT, SuspicionTier.HOT));
  }

  @Test
  void shouldEvaluate_corePunishAlwaysEvaluatedAtBaseline() {
    ActionPolicyService svc = new ActionPolicyService("default");
    assertTrue(svc.shouldEvaluate("SpeedEnvelope", CheckCategory.MOVEMENT, SuspicionTier.BASELINE));
    assertTrue(svc.shouldEvaluate("SpeedEnvelope", CheckCategory.MOVEMENT, SuspicionTier.HOT));
  }

  // ── tierFor ──────────────────────────────────────────────────────────

  @Test
  void tierFor_highRiskIsHot() {
    ActionPolicyService svc = new ActionPolicyService("default");
    assertEquals(SuspicionTier.HOT, svc.tierFor(8.0, 0.5, 0.0));
  }

  @Test
  void tierFor_lowTrustIsHot() {
    ActionPolicyService svc = new ActionPolicyService("default");
    assertEquals(SuspicionTier.HOT, svc.tierFor(0.0, 0.10, 0.0));
  }

  @Test
  void tierFor_moderateRiskIsElevated() {
    ActionPolicyService svc = new ActionPolicyService("default");
    assertEquals(SuspicionTier.ELEVATED, svc.tierFor(4.0, 0.6, 0.0));
  }

  @Test
  void tierFor_cleanPlayerIsBaseline() {
    ActionPolicyService svc = new ActionPolicyService("default");
    assertEquals(SuspicionTier.BASELINE, svc.tierFor(0.5, 0.8, 0.1));
  }
}
