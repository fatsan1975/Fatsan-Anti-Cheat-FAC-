package io.fatsan.fac.engine;

import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.EvidenceRecord;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.nextlevel.NextLevelAntiCheatPlatform;
import io.fatsan.fac.packet.PacketIntakeService;
import io.fatsan.fac.service.ActionPolicyService;
import io.fatsan.fac.service.CorroborationService;
import io.fatsan.fac.service.LagAwareThresholdService;
import io.fatsan.fac.service.PacketContext;
import io.fatsan.fac.service.PlayerExemptionService;
import io.fatsan.fac.service.PlayerGraceService;
import io.fatsan.fac.service.PlayerTrustService;
import io.fatsan.fac.service.RiskService;
import io.fatsan.fac.service.SuspicionPatternService;
import io.fatsan.fac.service.SuspicionTier;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

public final class AntiCheatEngine {
  private final PacketIntakeService packetIntakeService;
  private final ActionService actionService;
  private final RiskService riskService;
  private final CorroborationService corroborationService;
  private final PlayerTrustService playerTrustService;
  private final SuspicionPatternService suspicionPatternService;
  private final ActionPolicyService actionPolicyService;
  private final NextLevelAntiCheatPlatform nextLevelPlatform;
  private final EvidenceService evidenceService;
  private final PlayerGraceService graceService;
  private final PlayerExemptionService exemptionService;
  private final LagAwareThresholdService lagService;
  private final PacketContext packetContext;
  private final LongAdder processedEvents = new LongAdder();
  private final LongAdder suspiciousResults = new LongAdder();

  public AntiCheatEngine(
      PacketIntakeService packetIntakeService,
      ActionService actionService,
      RiskService riskService,
      CorroborationService corroborationService,
      PlayerTrustService playerTrustService,
      SuspicionPatternService suspicionPatternService,
      ActionPolicyService actionPolicyService,
      NextLevelAntiCheatPlatform nextLevelPlatform,
      EvidenceService evidenceService,
      PlayerGraceService graceService,
      PlayerExemptionService exemptionService,
      LagAwareThresholdService lagService,
      PacketContext packetContext) {
    this.packetIntakeService = packetIntakeService;
    this.actionService = actionService;
    this.riskService = riskService;
    this.corroborationService = corroborationService;
    this.playerTrustService = playerTrustService;
    this.suspicionPatternService = suspicionPatternService;
    this.actionPolicyService = actionPolicyService;
    this.nextLevelPlatform = nextLevelPlatform;
    this.evidenceService = evidenceService;
    this.graceService = graceService;
    this.exemptionService = exemptionService;
    this.lagService = lagService;
    this.packetContext = packetContext;
  }

  public void start() {
    packetIntakeService.setConsumer(this::onEvent);
  }

  public void stop() {
    packetIntakeService.setConsumer(event -> {});
  }

  public long processedEvents() {
    return processedEvents.sum();
  }

  public long suspiciousResults() {
    return suspiciousResults.sum();
  }

  /** Total check evaluations run across all events since startup. */
  public long totalEvaluations() {
    return packetIntakeService.registry().totalEvaluations();
  }

  /** Total check evaluations skipped by tier/policy gating since startup. */
  public long totalSkipped() {
    return packetIntakeService.registry().totalSkipped();
  }

  /** Total registered checks in the registry. */
  public int registeredCheckCount() {
    return packetIntakeService.registry().registeredCheckCount();
  }

  /**
   * Returns a snapshot of recent evidence records for the given player.
   * Returns an empty list if no evidence has been recorded or the player is unknown.
   */
  public List<EvidenceRecord> evidenceSnapshot(String playerId) {
    return evidenceService.snapshot(playerId);
  }

  /**
   * Releases all per-player state accumulated by the risk, trust, suspicion
   * pattern, and corroboration services when a player disconnects.
   *
   * <p>Check-level state (buffers and window trackers) is handled separately
   * via {@link io.fatsan.fac.engine.CheckRegistry#clearPlayer(String)}, which
   * is called directly from
   * {@link io.fatsan.fac.packet.BukkitSignalBridge#onQuit}.
   *
   * @param playerId the disconnecting player's UUID string
   */
  public void clearPlayer(String playerId) {
    riskService.clearPlayer(playerId);
    playerTrustService.clearPlayer(playerId);
    suspicionPatternService.clearPlayer(playerId);
    corroborationService.clearPlayer(playerId);
    evidenceService.clearPlayer(playerId);
    actionService.clearPlayer(playerId);
    nextLevelPlatform.clearPlayer(playerId);
    packetContext.clearPlayer(playerId);
  }

  private void onEvent(NormalizedEvent event) {
    processedEvents.increment();

    // Gate 1: Full exemption — NPCs, spectating staff, bypass-permission holders
    if (exemptionService.isExempt(event.playerId())) {
      return;
    }

    // Gate 2: Grace period — suppress detection right after login/teleport/setback
    // Prevents false positives during chunk loading and position desync windows.
    if (graceService.isUnderGrace(event.playerId())) {
      playerTrustService.onCleanEvent(event.playerId());
      return;
    }

    // Gate 3: Extreme server lag — skip detection below 8 TPS
    // At extreme lag, movement deltas are so distorted that any flag is a false positive.
    if (lagService.isLaggingTooMuch()) {
      playerTrustService.onCleanEvent(event.playerId());
      return;
    }

    nextLevelPlatform.onEvent(event);
    SuspicionTier tier =
        actionPolicyService.tierFor(
            riskService.currentRisk(event.playerId()),
            playerTrustService.trustScore(event.playerId()),
            suspicionPatternService.recentIntensity(event.playerId()));
    List<CheckResult> results = packetIntakeService.registry().evaluateAll(event, tier, actionPolicyService);
    if (results.isEmpty()) {
      playerTrustService.onCleanEvent(event.playerId());
      suspicionPatternService.onCleanEvent(event.playerId());
      riskService.coolDown(event.playerId());
      return;
    }

    for (CheckResult result : results) {
      suspiciousResults.increment();
      playerTrustService.onSuspicion(event.playerId(), result);
      corroborationService.record(event.playerId(), result);
      double multiplier =
          playerTrustService.riskMultiplier(event.playerId())
              * suspicionPatternService.onSuspicion(event.playerId(), result);
      double risk = riskService.apply(event.playerId(), result, multiplier);
      nextLevelPlatform.onResult(event.playerId(), result, risk);
      actionService.handleSuspicion(event.playerId(), result, risk);
    }
  }
}
