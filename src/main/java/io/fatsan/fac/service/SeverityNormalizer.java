package io.fatsan.fac.service;

import io.fatsan.fac.model.CheckCategory;
import java.util.Locale;

/**
 * Normalises raw check severity scores on a per-family basis before they are
 * fed into the risk accumulator.
 *
 * <p>Without normalisation, check families with loose severity ranges (e.g.
 * deep item-context or via-derived statistical families) can dominate the
 * risk score and produce false-positive punishments even when the underlying
 * signal is ambiguous or environment-specific.
 *
 * <p>Each family is assigned a maximum severity cap that reflects its
 * detection confidence and false-positive risk profile.  Core punish
 * candidates (speed, nofall, reach, scaffold, fast-break) are allowed a
 * full cap of {@code 1.0}, while noisy statistical and via-derived families
 * are capped at lower values so that their risk contribution stays
 * proportional to their actual confidence level.
 *
 * <p>The caps below are aligned with the action-safety matrix defined in
 * {@code docs/ACTION_SAFETY_MATRIX.md} and the profile taxonomy in
 * {@code docs/COMPATIBILITY_PROFILES.md}.
 */
public final class SeverityNormalizer {

  private SeverityNormalizer() {}

  /**
   * Returns the severity value after applying the family-level cap.
   *
   * @param checkName  the check's canonical name (case-insensitive)
   * @param category   the check category
   * @param rawSeverity the raw severity in [0, N] produced by the check
   * @return the capped severity, always in [0, cap(family)]
   */
  public static double normalize(
      String checkName, CheckCategory category, double rawSeverity) {
    double cap = familyCap(checkName, category);
    return Math.max(0.0, Math.min(cap, rawSeverity));
  }

  // ── Family caps ──────────────────────────────────────────────────────────

  private static double familyCap(String checkName, CheckCategory category) {
    String n = checkName.toLowerCase(Locale.ROOT);

    // ── Deep item context: high false-positive risk on custom-item servers ──
    if (isDeepItemContext(n)) {
      return 0.45;
    }

    // ── Via / timing-derived: signal quality depends heavily on proxy setup ──
    if (isViaDerived(n) || isTimingDerived(n)) {
      return 0.55;
    }

    // ── Protocol noise: keepalive, ping, traffic ─────────────────────────────
    // Checked before statistical so that protocol-noise checks that also
    // contain statistical keywords (e.g. "KeepAliveDrift") get the higher cap.
    if (isProtocolNoiseFamily(n, category)) {
      return 0.70;
    }

    // ── Statistical families: entropy, variance, collapse, plateau ──────────
    // These families can fire on legitimate lag patterns; keep contribution
    // bounded so corroboration is required to escalate risk meaningfully.
    if (isStatisticalFamily(n)) {
      return 0.65;
    }

    // ── Core punish candidates: highest confidence, full severity allowed ────
    if (isCorePunishCandidate(n)) {
      return 1.0;
    }

    // ── Fallback caps by category ────────────────────────────────────────────
    return switch (category) {
      case MOVEMENT -> 0.90;
      case COMBAT   -> 0.85;
      case WORLD    -> 0.80;
      case INVENTORY -> 0.70;
      case PROTOCOL  -> 0.65;
    };
  }

  // ── Family membership predicates ─────────────────────────────────────────

  private static boolean isDeepItemContext(String n) {
    return n.contains("attribute")
        || n.contains("enchant")
        || n.contains("meta")
        || n.contains("lore")
        || n.contains("customitem")
        || n.contains("unbreakable")
        || n.contains("signaturelock")
        || n.contains("deepcontext");
  }

  private static boolean isViaDerived(String n) {
    return n.contains("via")
        || n.contains("rewrite")
        || n.contains("smoothing")
        || n.contains("windowsnap")
        || n.contains("smear");
  }

  private static boolean isTimingDerived(String n) {
    return n.contains("timingmismatch")
        || n.contains("packetorder")
        || n.contains("bundleorder")
        || n.contains("bundle")
        || n.contains("latency")
        || n.contains("profilemismatch");
  }

  private static boolean isStatisticalFamily(String n) {
    return n.contains("entropy")
        || n.contains("variance")
        || n.contains("collapse")
        || n.contains("plateau")
        || n.contains("oscillation")
        || n.contains("modulo")
        || n.contains("drift")
        || n.contains("cadence")
        || n.contains("intervallock");
  }

  private static boolean isProtocolNoiseFamily(String n, CheckCategory category) {
    return category == CheckCategory.PROTOCOL
        || n.contains("keepalive")
        || n.contains("ping")
        || n.contains("traffic")
        || n.contains("teleport");
  }

  private static boolean isCorePunishCandidate(String n) {
    return n.equals("speedenvelope")
        || n.equals("verticalmotionenvelope")
        || n.equals("nofall")
        || n.equals("nofallheuristic")
        || n.equals("impossiblegroundtransition")
        || n.equals("reachheuristic")
        || n.equals("impossiblecritical")
        || n.equals("fastbreak")
        || n.equals("scaffoldpattern")
        || n.equals("noslow")
        || n.equals("jesus")
        || n.equals("antikb")
        || n.equals("movementphysics")
        || n.equals("phase")
        || n.equals("velocitymanipulation")
        || n.equals("glidemimic")
        || n.equals("reachraycast")
        || n.equals("step")
        || n.equals("boatfly")
        || n.equals("spider")
        || n.equals("tower")
        || n.equals("autototem")
        || n.equals("multitargetaura")
        || n.equals("autocrystal")
        // ── Iteration 11 core candidates ─────────────────────────────────────
        || n.equals("noswing")
        || n.equals("throughwallhit")
        || n.equals("forcefield")
        || n.equals("flightsustained")
        || n.equals("airjump")
        || n.equals("noclip")
        || n.equals("longjump")
        || n.equals("antivoid")
        || n.equals("dashhack")
        || n.equals("nuker")
        || n.equals("instantbreakhardrock")
        // ── Iteration 13: Phase 2 bypass closing ─────────────────────────────
        || n.equals("groundtruthmismatch")
        || n.equals("packettimer")
        || n.equals("eyetohitboxreach")
        || n.equals("invalidposition")
        || n.equals("velocityhorizontal")
        || n.equals("velocityvertical")
        || n.equals("velocitydelayed")
        || n.equals("diagonalscaffold")
        || n.equals("expandscaffold")
        || n.equals("safewalkedge");
  }
}
