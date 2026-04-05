package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.service.ActionDisposition;
import io.fatsan.fac.service.ActionPolicyService;
import io.fatsan.fac.service.SuspicionTier;
import org.junit.jupiter.api.Test;

class ActionPolicyServiceTest {
  @Test
  void shouldKeepViaDerivedChecksReviewOnlyInDefaultProfile() {
    ActionPolicyService service = new ActionPolicyService("default");

    ActionPolicyService.ResolvedPolicy policy = service.resolve("CombatViaWindowSmear", CheckCategory.COMBAT);

    assertEquals(ActionDisposition.REVIEW_ONLY, policy.disposition());
    assertEquals(SuspicionTier.HOT, policy.minimumEvaluationTier());
  }

  @Test
  void shouldAllowCorePunishChecksAtBaselineInStrictProfile() {
    ActionPolicyService service = new ActionPolicyService("strict");

    ActionPolicyService.ResolvedPolicy policy = service.resolve("SpeedEnvelope", CheckCategory.MOVEMENT);

    assertEquals(ActionDisposition.CORROBORATED_KICK, policy.disposition());
    assertEquals(SuspicionTier.BASELINE, policy.minimumEvaluationTier());
  }

  @Test
  void shouldDisableDeepItemChecksInCustomMechanicsSafeProfile() {
    ActionPolicyService service = new ActionPolicyService("custom-mechanics-safe");

    ActionPolicyService.ResolvedPolicy policy = service.resolve("BlockBreakCommandLoreAnomaly", CheckCategory.WORLD);

    assertEquals(ActionDisposition.DISABLED_BY_DEFAULT, policy.disposition());
    assertFalse(service.shouldEvaluate("BlockBreakCommandLoreAnomaly", CheckCategory.WORLD, SuspicionTier.HOT));
  }

  @Test
  void shouldEscalateSuspicionTierFromRiskTrustAndRecentIntensity() {
    ActionPolicyService service = new ActionPolicyService("default");

    assertEquals(SuspicionTier.BASELINE, service.tierFor(0.5D, 0.8D, 0.5D));
    assertEquals(SuspicionTier.ELEVATED, service.tierFor(3.2D, 0.6D, 0.3D));
    assertEquals(SuspicionTier.HOT, service.tierFor(1.0D, 0.15D, 0.0D));
    assertTrue(service.shouldEvaluate("ReachVarianceCollapse", CheckCategory.COMBAT, SuspicionTier.HOT));
    assertFalse(service.shouldEvaluate("ReachVarianceCollapse", CheckCategory.COMBAT, SuspicionTier.BASELINE));
  }
}
