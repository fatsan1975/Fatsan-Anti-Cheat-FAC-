package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.RotationEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TriggerBot Detection (154)
 * Entity görüş alanına girdiğinde otomatik saldırı tespiti
 */
public class TriggerBotCheck implements Check {
  private final int bufferLimit;
  private final Map<String, PlayerAttackData> playerData = new ConcurrentHashMap<>();

  public TriggerBotCheck(int bufferLimit) {
    this.bufferLimit = bufferLimit;
  }

  @Override
  public String name() {
    return "TriggerBot";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (event instanceof CombatHitEvent hit) {
      return evaluateAttack(hit);
    } else if (event instanceof RotationEvent rot) {
      return evaluateRotation(rot);
    }
    return CheckResult.clean(name(), category());
  }

  private CheckResult evaluateAttack(CombatHitEvent hit) {
    PlayerAttackData data = playerData.computeIfAbsent(hit.playerId(), k -> new PlayerAttackData());

    long now = hit.nanoTime();
    long timeSinceRotation = now - data.lastRotationTime;

    // TriggerBot attacks immediately after rotation
    if (timeSinceRotation > 0 && timeSinceRotation < 5_000_000) { // 5ms
      data.triggerBotSuspicion++;

      if (data.triggerBotSuspicion > bufferLimit) {
        return new CheckResult(true, name(), category(), "TriggerBot detected (attack-delay=" + timeSinceRotation + "ns)", 0.8, false);
      }
    } else {
      data.triggerBotSuspicion = Math.max(0, data.triggerBotSuspicion - 1);
    }

    if (data.firstAttackTime == 0) {
      data.firstAttackTime = now;
    }

    data.lastAttackTime = now;
    data.attackCount++;

    // Check for consistent timing
    if (data.attackCount > 5) {
      long avgInterval = (data.lastAttackTime - data.firstAttackTime) / data.attackCount;
      long variance = Math.abs(timeSinceRotation - avgInterval);

      if (variance < 1_000_000) { // Very consistent timing
        return new CheckResult(true, name(), category(), "TriggerBot consistent timing detected", 0.7, false);
      }
    }

    return CheckResult.clean(name(), category());
  }

  private CheckResult evaluateRotation(RotationEvent rot) {
    PlayerAttackData data = playerData.computeIfAbsent(rot.playerId(), k -> new PlayerAttackData());

    data.lastRotationTime = rot.nanoTime();
    data.lastDeltaYaw = rot.deltaYaw();
    data.lastDeltaPitch = rot.deltaPitch();

    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    playerData.remove(playerId);
  }

  private static class PlayerAttackData {
    volatile long lastAttackTime;
    volatile long lastRotationTime;
    volatile long firstAttackTime;
    volatile int attackCount;
    volatile int triggerBotSuspicion;
    volatile float lastDeltaYaw;
    volatile float lastDeltaPitch;
  }
}
