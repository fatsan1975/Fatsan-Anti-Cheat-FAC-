package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.PacketFlyingEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blink Detection (186-188)
 * Paket geciktirme/sahtekarlık tespiti
 */
public class BlinkCheck implements Check {
  private final int bufferLimit;
  private final Map<String, BlinkData> playerData = new ConcurrentHashMap<>();

  public BlinkCheck(int bufferLimit) {
    this.bufferLimit = bufferLimit;
  }

  @Override
  public String name() {
    return "Blink";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (event instanceof PacketFlyingEvent flying) {
      return evaluateFlying(flying);
    } else if (event instanceof MovementEvent move) {
      return evaluateMovement(move);
    } else if (event instanceof CombatHitEvent hit) {
      return evaluateCombat(hit);
    }
    return CheckResult.clean(name(), category());
  }

  private CheckResult evaluateFlying(PacketFlyingEvent flying) {
    BlinkData data = playerData.computeIfAbsent(flying.playerId(), k -> new BlinkData());

    long now = flying.nanoTime();

    // Track packet intervals
    if (data.lastPacketTime > 0) {
      long interval = now - data.lastPacketTime;

      // Normal interval is ~50ms (20 TPS)
      if (interval > 200_000_000) { // 200ms gap
        data.blinkSuspicion++;
        data.lastBlinkTime = now;

        if (data.blinkSuspicion > bufferLimit) {
          return new CheckResult(true, name(), category(), "Blink packet delay detected (gap=" + interval + "ns)", 0.8, false);
        }
      } else if (interval < 100_000_000) {
        data.blinkSuspicion = Math.max(0, data.blinkSuspicion - 1);
      }
    }

    data.lastPacketTime = now;
    data.packetCount++;

    return CheckResult.clean(name(), category());
  }

  private CheckResult evaluateMovement(MovementEvent move) {
    BlinkData data = playerData.get(move.playerId());
    if (data == null) {
      return CheckResult.clean(name(), category());
    }

    // Check for position jump after blink
    if (data.lastBlinkTime > 0) {
      long timeSinceBlink = move.nanoTime() - data.lastBlinkTime;

      if (timeSinceBlink < 50_000_000) { // Within 50ms of blink end
        double delta = move.deltaXZ();

        // Large movement after blink = position jump
        if (delta > 3.0) {
          data.positionJumps++;
          if (data.positionJumps > 2) {
            return new CheckResult(true, name(), category(), "Blink position jump detected (delta=" + String.format("%.2f", delta) + ")", 0.85, false);
          }
        }
      }
    }

    // Track movement for teleport detection
    if (data.lastX != 0 || data.lastZ != 0) {
      double distance = Math.sqrt(
          Math.pow(move.deltaXZ() - data.lastX, 2) +
              Math.pow(move.deltaXZ() - data.lastZ, 2)
      );

      if (distance > 5.0) { // Teleport-like movement
        data.teleportCount++;
        if (data.teleportCount > 3) {
          return new CheckResult(true, name(), category(), "Blink teleport pattern detected", 0.75, false);
        }
      }
    }

    return CheckResult.clean(name(), category());
  }

  private CheckResult evaluateCombat(CombatHitEvent hit) {
    BlinkData data = playerData.get(hit.playerId());
    if (data == null) {
      return CheckResult.clean(name(), category());
    }

    // Check for attacks during suspected blink
    if (data.lastBlinkTime > 0) {
      long timeSinceBlink = hit.nanoTime() - data.lastBlinkTime;

      // Attack immediately after blink ends
      if (timeSinceBlink > 0 && timeSinceBlink < 20_000_000) {
        data.blinkAttacks++;
        if (data.blinkAttacks > 2) {
          return new CheckResult(true, name(), category(), "Blink attack delay detected", 0.8, false);
        }
      }
    }

    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    playerData.remove(playerId);
  }

  private static class BlinkData {
    volatile long lastPacketTime;
    volatile long lastBlinkTime;
    volatile int blinkSuspicion;
    volatile int packetCount;
    volatile int positionJumps;
    volatile int teleportCount;
    volatile int blinkAttacks;
    volatile double lastX;
    volatile double lastZ;
  }
}
