package io.fatsan.fac.nextlevel;

import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class NextLevelAntiCheatPlatform implements CheckExecutionObserver {
  private final AtomicLong events = new AtomicLong();
  private final AtomicLong suspiciousEvents = new AtomicLong();
  private final AtomicLong parseErrors = new AtomicLong();
  private final AtomicLong outOfOrder = new AtomicLong();
  private final AtomicLong missingField = new AtomicLong();
  private final Map<String, Long> lastEventNanos = new ConcurrentHashMap<>();
  private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
  private final Map<String, String> traceByAction = new ConcurrentHashMap<>();
  private final Map<String, String> labels = new ConcurrentHashMap<>();
  private final Map<String, FalsePositiveRecord> falsePositiveFeedback = new ConcurrentHashMap<>();
  private final Map<String, String> lastResultByPlayer = new ConcurrentHashMap<>();
  private final Map<String, Deque<RetentionRecord>> retention = new ConcurrentHashMap<>();
  private final Set<String> shadowFlagged = ConcurrentHashMap.newKeySet();
  private final Map<String, CheckLifecycleState> lifecycleByCheck = new ConcurrentHashMap<>();
  private final Map<String, Double> confidenceByCheck = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> dependencyGraph = new ConcurrentHashMap<>();
  private final Map<String, String> policyDsl = new ConcurrentHashMap<>();
  private final Map<String, ThresholdProfile> versionThresholds = new ConcurrentHashMap<>();
  private final Map<String, String> abDeployment = new ConcurrentHashMap<>();
  private final Map<String, Long> latencyByCheckNanos = new ConcurrentHashMap<>();
  private final Map<String, Long> failuresByCheck = new ConcurrentHashMap<>();

  public void onEvent(NormalizedEvent event) {
    events.incrementAndGet();
    String player = event.playerId();
    sessionContext(player, event.nanoTime());
    if (event.nanoTime() <= 0L) {
      missingField.incrementAndGet();
    }
    Long previous = lastEventNanos.put(player, event.nanoTime());
    if (previous != null && event.nanoTime() < previous) {
      outOfOrder.incrementAndGet();
    }
    retentionWrite(player, event.nanoTime(), "event");
  }

  public void onResult(String playerId, CheckResult result, double risk) {
    suspiciousEvents.incrementAndGet();
    lastResultByPlayer.put(playerId, result.checkName());
    confidenceByCheck.putIfAbsent(result.checkName(), Math.min(1.0D, 0.5D + (result.severity() / 20.0D)));
    lifecycleByCheck.putIfAbsent(result.checkName(), CheckLifecycleState.ACTIVE);
    if (risk >= 9.0D) {
      shadowFlagged.add(playerId);
    }
    retentionWrite(playerId, System.nanoTime(), "result:" + result.checkName());
  }


  @Override
  public void onCheckExecution(String checkName, NormalizedEvent event, long latencyNanos, boolean failed) {
    latencyByCheckNanos.merge(checkName, latencyNanos, Math::max);
    if (failed) {
      failuresByCheck.merge(checkName, 1L, Long::sum);
      lifecycleByCheck.put(checkName, CheckLifecycleState.COOLDOWN);
    } else {
      if (latencyNanos > 2_000_000L) {
        lifecycleByCheck.putIfAbsent(checkName, CheckLifecycleState.WARMUP);
      } else {
        lifecycleByCheck.putIfAbsent(checkName, CheckLifecycleState.ACTIVE);
      }
    }
  }
  public int schemaVersion() { return 2; }
  public TickSync tickSync(long serverTick, long regionTick, long nanoTimestamp) { return new TickSync(serverTick, regionTick, nanoTimestamp); }
  public SessionContext sessionContext(String playerId, long nowNanos) { return sessions.computeIfAbsent(playerId, key -> new SessionContext(nowNanos, "UNKNOWN", 0.0D, 20.0D)); }
  public String traceIdForAction(String actionKey) { return traceByAction.computeIfAbsent(actionKey, key -> UUID.randomUUID().toString()); }
  public void pushGroundTruthLabel(String playerId, String label) { labels.put(playerId, label); }
  public void markFalsePositive(String playerId, String reason) {
    falsePositiveFeedback.put(
        playerId,
        new FalsePositiveRecord(
            reason == null ? "manual" : reason,
            lastResultByPlayer.getOrDefault(playerId, "unknown"),
            Instant.now().toEpochMilli()));
  }
  public RetentionTier retentionTier(long ageMillis) { if (ageMillis <= 86_400_000L) return RetentionTier.HOT; if (ageMillis <= 604_800_000L) return RetentionTier.WARM; return RetentionTier.COLD; }
  public boolean shouldSample(String playerId, boolean suspicious) {
    if (suspicious) return true;
    if (playerId == null || playerId.isBlank()) {
      parseErrors.incrementAndGet();
      return false;
    }
    int bucket = Math.floorMod(playerId.hashCode(), 100);
    return bucket < 20;
  }

  public String privacySafePlayerId(String playerId) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] raw = digest.digest(playerId.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(raw).substring(0, 22);
    } catch (Exception exception) {
      parseErrors.incrementAndGet();
      return "hash_error";
    }
  }

  public DataQualitySnapshot dataQuality() { return new DataQualitySnapshot(events.get(), parseErrors.get(), outOfOrder.get(), missingField.get()); }
  public void setLifecycle(String checkName, CheckLifecycleState state) { lifecycleByCheck.put(checkName, state); }
  public void setCheckConfidence(String checkName, double confidence) { confidenceByCheck.put(checkName, Math.max(0.0D, Math.min(1.0D, confidence))); }
  public WindowSnapshot window(String key, WindowType type, double value) { return new WindowSnapshot(key, type, value, Instant.now().toEpochMilli()); }
  public void linkChecks(String sourceCheck, String dependentCheck) { dependencyGraph.computeIfAbsent(sourceCheck, key -> new HashSet<>()).add(dependentCheck); }
  public void registerPolicyDsl(String checkName, String expression) { policyDsl.put(checkName, expression); }
  public void setThresholdProfile(String protocolVersion, ThresholdProfile profile) { versionThresholds.put(protocolVersion, profile); }
  public long adaptiveCooldownMillis(boolean lagSpike, boolean teleportBurst) { long base = 800L; if (lagSpike) base += 700L; if (teleportBurst) base += 600L; return base; }
  public String profileKey(String worldName, String gameMode) { return worldName + "::" + gameMode; }
  public HealthSnapshot selfHealth() {
    long maxLatency = latencyByCheckNanos.values().stream().mapToLong(Long::longValue).max().orElse(0L);
    long failures = failuresByCheck.values().stream().mapToLong(Long::longValue).sum();
    return new HealthSnapshot(events.get(), parseErrors.get(), lifecycleByCheck.size(), maxLatency, failures);
  }
  public void setAbDeployment(String checkName, String cohort) { abDeployment.put(checkName, cohort); }

  public double deterministicMovementDelta(double input, double drag) { return input * (1.0D - drag); }
  public InputInference inferInput(double deltaXZ, double deltaY) { return new InputInference(deltaXZ > 0.18D, deltaY > 0.0D, Math.abs(deltaXZ) > 0.08D); }
  public CollisionContext collisionContext(boolean slab, boolean stairs, boolean honey, boolean slime, boolean water, boolean bubbleColumn) { return new CollisionContext(slab, stairs, honey, slime, water, bubbleColumn); }
  public double potionAdjustedSpeed(double base, int speedAmp, int slowAmp) { return base * (1.0D + (speedAmp * 0.2D) - (slowAmp * 0.15D)); }
  public double lagCompensatedTolerance(double tolerance, double tps) { return tps >= 19.0D ? tolerance : tolerance * (20.0D / Math.max(10.0D, tps)); }
  public double jitterAdjusted(double value, double jitter) { return value - Math.min(Math.abs(value) * 0.35D, jitter); }
  public double trajectoryConsistency(List<Double> deltas) { return 1.0D - varianceRatio(deltas); }
  public boolean jumpArcAnomaly(double ascent, double descent) { return ascent > 0.7D || descent < -1.5D; }
  public boolean isIllegalMicroTeleport(double distance, boolean pluginTeleportFlag) { return !pluginTeleportFlag && distance > 0.9D; }
  public boolean setbackSafety(boolean destinationSafe, boolean antiStuckOk) { return destinationSafe && antiStuckOk; }

  public double normalizeReach(double rawReach, double interpolationComp) { return Math.max(0.0D, rawReach - interpolationComp); }
  public double aimClusterScore(List<Float> yawChanges) { return 1.0D - varianceRatioFloat(yawChanges); }
  public boolean snapSmoothHybridDetected(float snapDelta, float smoothStdDev) { return snapDelta > 20F && smoothStdDev < 0.3F; }
  public double clickEntropyScore(List<Long> intervals) { return 1.0D - varianceRatioLong(intervals); }
  public double targetSwitchSemanticScore(int switches, long windowMillis) { return (switches * 1000.0D) / Math.max(1L, windowMillis); }
  public boolean criticalLegit(boolean airborne, boolean serverCriticalWindow) { return airborne && serverCriticalWindow; }
  public double combatContextFusion(double ping, double knockback, double cps, double yawPitchSync) { return (ping * 0.1D) + (knockback * 0.2D) + (cps * 0.3D) + (yawPitchSync * 0.4D); }
  public boolean comboImpossible(int hitsInBurst, long burstMillis) { return hitsInBurst >= 6 && burstMillis < 400L; }
  public double fightSessionScore(List<Double> riskSamples) { return riskSamples.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D); }
  public ReplayVerdict replayVerdict(double sessionScore, double consistency) { return sessionScore > 6.5D && consistency > 0.65D ? ReplayVerdict.SUSPICIOUS : ReplayVerdict.CLEAN; }

  public double riskFusionBayesian(double priorRisk, double confidence) { return priorRisk * (0.6D + (0.4D * confidence)); }
  public double corroborationV2(int categories, int independentChecks, long proximityMillis) { return (categories * 1.5D) + independentChecks - (proximityMillis / 4000.0D); }
  public EnforcementStage progressiveEnforcement(double risk) { if (risk >= 10) return EnforcementStage.HARD_ACTION; if (risk >= 7) return EnforcementStage.SOFT_SETBACK; if (risk >= 4) return EnforcementStage.SHADOW_FLAG; return EnforcementStage.ALERT; }
  public boolean isShadowFlagged(String playerId) { return shadowFlagged.contains(playerId); }
  public Map<String, Double> featureStoreSnapshot(String playerId, double risk, double trust) { Map<String, Double> out = new HashMap<>(); out.put("risk", risk); out.put("trust", trust); out.put("shadow", shadowFlagged.contains(playerId) ? 1.0D : 0.0D); return out; }
  public GovernanceSnapshot modelGovernanceSnapshot(long driftSignals, long rollbacks) { return new GovernanceSnapshot(driftSignals, rollbacks, driftSignals == 0 ? "stable" : "monitor"); }
  public Map<String, Long> feedbackSummaryByCheck() {
    Map<String, Long> summary = new HashMap<>();
    for (FalsePositiveRecord record : falsePositiveFeedback.values()) {
      summary.merge(record.checkName(), 1L, Long::sum);
    }
    return summary;
  }
  public PremiumDashboardSnapshot premiumDashboard() { return new PremiumDashboardSnapshot(events.get(), suspiciousEvents.get(), falsePositiveFeedback.size()); }
  public String incidentProfileSwitch(boolean exploitWaveDetected) { return exploitWaveDetected ? "safe" : "balanced"; }
  public PerformanceSloSnapshot performanceSlo(double p95EvalMs, long memoryBytes, double perPlayerCpuMicros) { return new PerformanceSloSnapshot(p95EvalMs, memoryBytes, perPlayerCpuMicros); }
  public boolean releaseGate(boolean noisyCheck, double fpRatio, double overrideRate) { return !noisyCheck && fpRatio <= 0.08D && overrideRate <= 0.12D; }

  public NextLevelStatus status() {
    return new NextLevelStatus(events.get(), suspiciousEvents.get(), labels.size(), falsePositiveFeedback.size(), shadowFlagged.size(), lifecycleByCheck.size());
  }

  /**
   * Releases all per-player transient telemetry state for the given player.
   *
   * <p>Labels and false-positive feedback are intentionally <em>not</em> cleared —
   * they are permanent operator annotations that should survive reconnects.
   * Check-level lifecycle, confidence, and latency data are also retained
   * as they are keyed by check name, not player id.
   *
   * @param playerId the disconnecting player's UUID string
   */
  public void clearPlayer(String playerId) {
    lastEventNanos.remove(playerId);
    sessions.remove(playerId);
    lastResultByPlayer.remove(playerId);
    retention.remove(playerId);
    shadowFlagged.remove(playerId);
    // traceByAction is keyed by action key (not pure playerId) — leave intact
  }

  private void retentionWrite(String playerId, long nanoTime, String type) {
    Deque<RetentionRecord> queue = retention.computeIfAbsent(playerId, key -> new ArrayDeque<>());
    queue.addLast(new RetentionRecord(nanoTime, type));
    while (queue.size() > 256) queue.removeFirst();
  }

  private static double varianceRatio(List<Double> values) {
    if (values == null || values.size() < 2) return 0.0D;
    double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
    double variance = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0.0D);
    return Math.min(1.0D, variance / Math.max(0.001D, Math.abs(mean)));
  }

  private static double varianceRatioFloat(List<Float> values) {
    if (values == null || values.size() < 2) return 0.0D;
    double mean = values.stream().mapToDouble(Float::doubleValue).average().orElse(0.0D);
    double variance = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0.0D);
    return Math.min(1.0D, variance / Math.max(0.001D, Math.abs(mean)));
  }

  private static double varianceRatioLong(List<Long> values) {
    if (values == null || values.size() < 2) return 0.0D;
    double mean = values.stream().mapToDouble(Long::doubleValue).average().orElse(0.0D);
    double variance = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0.0D);
    return Math.min(1.0D, variance / Math.max(1.0D, Math.abs(mean)));
  }

  public record TickSync(long serverTick, long regionTick, long nanoTimestamp) {}
  public record SessionContext(long sessionStartNanos, String protocolProfile, double pingBaseline, double tpsBaseline) {}
  public record DataQualitySnapshot(long events, long parseErrors, long outOfOrder, long missingFields) {}
  public enum CheckLifecycleState { INIT, WARMUP, ACTIVE, COOLDOWN, RETIRED }
  public enum WindowType { SLIDING, TUMBLING, EWMA }
  public record WindowSnapshot(String key, WindowType type, double value, long capturedAtMillis) {}
  public record ThresholdProfile(double movement, double combat, double world) {}
  public record HealthSnapshot(long processedEvents, long parseErrors, long checksTracked, long worstCheckLatencyNanos, long totalCheckFailures) {}
  public record InputInference(boolean sprintLikely, boolean jumpLikely, boolean strafeLikely) {}
  public record CollisionContext(boolean slab, boolean stairs, boolean honey, boolean slime, boolean water, boolean bubbleColumn) {}
  public enum ReplayVerdict { CLEAN, SUSPICIOUS }
  public enum EnforcementStage { ALERT, SHADOW_FLAG, SOFT_SETBACK, HARD_ACTION }
  public record GovernanceSnapshot(long driftSignals, long rollbacks, String state) {}
  public record PremiumDashboardSnapshot(long totalEvents, long suspiciousEvents, long falsePositiveReports) {}
  public record PerformanceSloSnapshot(double p95EvalMs, long memoryBytes, double perPlayerCpuMicros) {}
  public record FalsePositiveRecord(String reason, String checkName, long recordedAtMillis) {}
  public enum RetentionTier { HOT, WARM, COLD }
  public record RetentionRecord(long nanoTime, String type) {}
  public record NextLevelStatus(long totalEvents, long suspiciousEvents, long labels, long falsePositiveFeedback, long shadowFlaggedPlayers, long trackedCheckLifecycles) {}
}
