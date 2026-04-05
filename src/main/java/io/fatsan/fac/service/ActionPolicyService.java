package io.fatsan.fac.service;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import java.util.Locale;

public final class ActionPolicyService {
  private final String profile;

  public ActionPolicyService(String profile) {
    this.profile = profile == null || profile.isBlank() ? "default" : profile.toLowerCase(Locale.ROOT);
  }

  public ResolvedPolicy resolve(CheckResult result) {
    return resolve(result.checkName(), result.category());
  }

  public ResolvedPolicy resolve(String checkName, CheckCategory category) {
    String normalized = checkName.toLowerCase(Locale.ROOT);

    // ── Deep item context ─────────────────────────────────────────────────
    // custom-mechanics-safe: fully disabled (RPG/modded servers)
    // default/survival:      review only, elevated tier
    if (isDeepItemContext(normalized)) {
      if (isCustomMechanicsSafe()) {
        return new ResolvedPolicy(ActionDisposition.DISABLED_BY_DEFAULT, SuspicionTier.HOT, "deep-item-context");
      }
      return new ResolvedPolicy(ActionDisposition.REVIEW_ONLY, SuspicionTier.HOT, "deep-item-context");
    }

    // ── Via-derived / timing-derived ─────────────────────────────────────
    // via-heavy profile: always review-only, higher tier requirement
    if (isViaDerived(normalized) || isTimingDerived(normalized)) {
      if (isViaHeavy()) {
        return new ResolvedPolicy(ActionDisposition.REVIEW_ONLY, SuspicionTier.HOT, "via-or-timing-derived");
      }
      return new ResolvedPolicy(ActionDisposition.REVIEW_ONLY,
          isStrict() ? SuspicionTier.ELEVATED : SuspicionTier.HOT, "via-or-timing-derived");
    }

    // ── Protocol noise (keepalive, ping, traffic, teleport) ───────────────
    // minigame: teleport-heavy, so teleport family is kept at review-only
    if (isMinigame() && isHighTeleportFamily(normalized)) {
      return new ResolvedPolicy(ActionDisposition.REVIEW_ONLY, SuspicionTier.ELEVATED, "minigame-teleport");
    }
    if (isProtocolNoiseFamily(normalized, category)) {
      return new ResolvedPolicy(ActionDisposition.ALERT_ONLY,
          isLightweight() ? SuspicionTier.HOT : SuspicionTier.ELEVATED, "protocol-noise");
    }

    // ── Core punish candidates ────────────────────────────────────────────
    // strict/practice/pvp: CORROBORATED_KICK
    // survival: same as default (CORROBORATED_SETBACK) but scaffold/fastbreak slightly more sensitive
    // minigame: scaffold/fastbreak less relevant, keep at alert-only
    if (isCorePunishCandidate(normalized)) {
      if (isMinigame() && isBuildWorldFamily(normalized)) {
        return new ResolvedPolicy(ActionDisposition.ALERT_ONLY, SuspicionTier.ELEVATED, "minigame-build-world");
      }
      ActionDisposition disp = isStrict()
          ? ActionDisposition.CORROBORATED_KICK
          : ActionDisposition.CORROBORATED_SETBACK;
      return new ResolvedPolicy(disp, SuspicionTier.BASELINE, "core-punish");
    }

    // ── Statistical families ──────────────────────────────────────────────
    // strict: can evaluate at ELEVATED tier
    // survival + build/world: ELEVATED (miners/nukers are more impactful in survival)
    // lightweight/minigame: pushed to HOT tier
    if (isRedundantStatFamily(normalized)) {
      if (isSurvival() && isBuildWorldFamily(normalized)) {
        return new ResolvedPolicy(ActionDisposition.ALERT_ONLY, SuspicionTier.ELEVATED, "survival-build-stat");
      }
      SuspicionTier minTier = isStrict() ? SuspicionTier.ELEVATED : SuspicionTier.HOT;
      if (isLightweight() || isMinigame()) minTier = SuspicionTier.HOT;
      return new ResolvedPolicy(ActionDisposition.ALERT_ONLY, minTier, "statistical-family");
    }

    // ── Core domain fallback ──────────────────────────────────────────────
    if (category == CheckCategory.MOVEMENT || category == CheckCategory.COMBAT
        || category == CheckCategory.WORLD) {
      return new ResolvedPolicy(ActionDisposition.ALERT_ONLY, SuspicionTier.BASELINE, "core-domain");
    }

    return new ResolvedPolicy(ActionDisposition.ALERT_ONLY, SuspicionTier.ELEVATED, "default");
  }

  public boolean shouldEvaluate(String checkName, CheckCategory category, SuspicionTier tier) {
    ResolvedPolicy policy = resolve(checkName, category);
    return policy.disposition() != ActionDisposition.DISABLED_BY_DEFAULT && tier.atLeast(policy.minimumEvaluationTier());
  }

  public SuspicionTier tierFor(double risk, double trust, double recentIntensity) {
    if (risk >= 7.5D || trust <= 0.20D || recentIntensity >= 5.0D) {
      return SuspicionTier.HOT;
    }
    if (risk >= 3.0D || trust <= 0.45D || recentIntensity >= 2.0D) {
      return SuspicionTier.ELEVATED;
    }
    return SuspicionTier.BASELINE;
  }

  public String profile() {
    return profile;
  }

  private boolean isStrict() {
    return "strict".equals(profile) || "practice".equals(profile) || "pvp".equals(profile);
  }

  private boolean isLightweight() {
    return "lightweight".equals(profile) || "safe".equals(profile);
  }

  private boolean isCustomMechanicsSafe() {
    return "custom-mechanics-safe".equals(profile);
  }

  private boolean isMinigame() {
    return "minigame".equals(profile);
  }

  private boolean isSurvival() {
    return "survival".equals(profile);
  }

  private boolean isViaHeavy() {
    return "via-heavy".equals(profile);
  }

  /** Teleport-heavy families that minigame profiles should treat conservatively. */
  private static boolean isHighTeleportFamily(String normalized) {
    return normalized.contains("teleport")
        || normalized.contains("regiontransition")
        || normalized.contains("bundleconfirm")
        || normalized.contains("regioniofusion");
  }

  /** Build/world-interaction families less relevant in minigame environments. */
  private static boolean isBuildWorldFamily(String normalized) {
    return normalized.contains("scaffold")
        || normalized.contains("fastbreak")
        || normalized.contains("blockbreak")
        || normalized.contains("blockplace")
        || normalized.startsWith("break");
  }

  private static boolean isViaDerived(String normalized) {
    return normalized.contains("via")
        || normalized.contains("rewrite")
        || normalized.contains("smoothing")
        || normalized.contains("window")
        || normalized.contains("smear")
        || normalized.contains("skew");
  }

  private static boolean isTimingDerived(String normalized) {
    return normalized.contains("packetorder")
        || normalized.contains("bundle")
        || normalized.contains("timingmismatch")
        || normalized.contains("latency")
        || normalized.contains("profilemismatch");
  }

  private static boolean isProtocolNoiseFamily(String normalized, CheckCategory category) {
    return category == CheckCategory.PROTOCOL
        || normalized.contains("keepalive")
        || normalized.contains("ping")
        || normalized.contains("traffic")
        || normalized.contains("teleport");
  }

  private static boolean isCorePunishCandidate(String normalized) {
    return normalized.contains("speedenvelope")
        || normalized.contains("verticalmotionenvelope")
        || normalized.contains("nofall")
        || normalized.contains("impossibleground")
        || normalized.contains("reachheuristic")
        || normalized.contains("impossiblecritical")
        || normalized.contains("fastbreak")
        || normalized.contains("scaffoldpattern")
        || normalized.equals("noslow")
        || normalized.equals("jesus")
        || normalized.equals("antikb")
        || normalized.equals("movementphysics")
        || normalized.equals("phase")
        || normalized.equals("velocitymanipulation")
        || normalized.equals("glidemimic")
        || normalized.equals("reachraycast")
        || normalized.equals("step")
        || normalized.equals("boatfly")
        || normalized.equals("spider")
        || normalized.equals("tower")
        || normalized.equals("autototem")
        || normalized.equals("multitargetaura")
        || normalized.equals("autocrystal")
        // ── Iteration 11 core punish candidates ──────────────────────────────
        || normalized.equals("noswing")
        || normalized.equals("throughwallhit")
        || normalized.equals("forcefield")
        || normalized.equals("flightsustained")
        || normalized.equals("airjump")
        || normalized.equals("noclip")
        || normalized.equals("longjump")
        || normalized.equals("antivoid")
        || normalized.equals("dashhack")
        || normalized.equals("nuker")
        || normalized.equals("instantbreakhardrock")
        // ── Iteration 13: Phase 2 bypass closing ─────────────────────────────
        || normalized.equals("groundtruthmismatch")
        || normalized.equals("packettimer")
        || normalized.equals("eyetohitboxreach")
        || normalized.equals("invalidposition")
        || normalized.equals("velocityhorizontal")
        || normalized.equals("velocityvertical")
        || normalized.equals("velocitydelayed")
        || normalized.equals("diagonalscaffold")
        || normalized.equals("expandscaffold")
        || normalized.equals("safewalkedge");
  }

  private static boolean isRedundantStatFamily(String normalized) {
    return normalized.contains("cadence")
        || normalized.contains("interval")
        || normalized.contains("burst")
        || normalized.contains("entropy")
        || normalized.contains("variance")
        || normalized.contains("collapse")
        || normalized.contains("plateau")
        || normalized.contains("lock")
        || normalized.contains("oscillation")
        || normalized.contains("modulo");
  }

  private static boolean isDeepItemContext(String normalized) {
    return normalized.contains("attribute")
        || normalized.contains("lore")
        || normalized.contains("enchant")
        || normalized.contains("meta")
        || normalized.contains("customitem")
        || normalized.contains("unbreakable");
  }

  public record ResolvedPolicy(
      ActionDisposition disposition, SuspicionTier minimumEvaluationTier, String family) {}
}
