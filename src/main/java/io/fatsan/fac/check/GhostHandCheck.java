package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PacketPlaceEvent;
import io.fatsan.fac.model.PacketDiggingEvent;
import io.fatsan.fac.model.RotationEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GhostHand Detection (177)
 * Duvar arkası etkileşim tespiti
 */
public class GhostHandCheck implements Check {
  private final int bufferLimit;
  private final Map<String, GhostHandData> playerData = new ConcurrentHashMap<>();

  public GhostHandCheck(int bufferLimit) {
    this.bufferLimit = bufferLimit;
  }

  @Override
  public String name() {
    return "GhostHand";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (event instanceof PacketPlaceEvent place) {
      return evaluatePlace(place);
    } else if (event instanceof PacketDiggingEvent dig) {
      return evaluateDig(dig);
    } else if (event instanceof RotationEvent rot) {
      return evaluateRotation(rot);
    }
    return CheckResult.clean(name(), category());
  }

  private CheckResult evaluatePlace(PacketPlaceEvent place) {
    GhostHandData data = playerData.computeIfAbsent(place.playerId(), k -> new GhostHandData());

    // Check if player is looking at the block
    if (data.hasRotation) {
      boolean canSee = canSeeBlock(data.accumulatedYaw, data.accumulatedPitch,
          place.blockPosition().getX(),
          place.blockPosition().getY(),
          place.blockPosition().getZ());

      if (!canSee) {
        data.ghostPlaces++;
        if (data.ghostPlaces > bufferLimit) {
          return new CheckResult(true, name(), category(), "GhostHand block place through wall detected", 0.85, false);
        }
      } else {
        data.ghostPlaces = Math.max(0, data.ghostPlaces - 1);
      }
    }

    data.lastPlaceTime = place.nanoTime();
    return CheckResult.clean(name(), category());
  }

  private CheckResult evaluateDig(PacketDiggingEvent dig) {
    if (dig.action() != PacketDiggingEvent.DiggingAction.START) {
      return CheckResult.clean(name(), category());
    }

    GhostHandData data = playerData.computeIfAbsent(dig.playerId(), k -> new GhostHandData());

    if (data.hasRotation) {
      boolean canSee = canSeeBlock(data.accumulatedYaw, data.accumulatedPitch,
          dig.blockPosition().getX(),
          dig.blockPosition().getY(),
          dig.blockPosition().getZ());

      if (!canSee) {
        data.ghostBreaks++;
        if (data.ghostBreaks > bufferLimit) {
          return new CheckResult(true, name(), category(), "GhostHand block break through wall detected", 0.85, false);
        }
      } else {
        data.ghostBreaks = Math.max(0, data.ghostBreaks - 1);
      }
    }

    return CheckResult.clean(name(), category());
  }

  private CheckResult evaluateRotation(RotationEvent rot) {
    GhostHandData data = playerData.computeIfAbsent(rot.playerId(), k -> new GhostHandData());

    // Accumulate deltas to track approximate facing direction
    data.accumulatedYaw += rot.deltaYaw();
    data.accumulatedPitch += rot.deltaPitch();
    data.hasRotation = true;
    data.lastRotationTime = rot.nanoTime();

    return CheckResult.clean(name(), category());
  }

  /**
   * Check if player can see a block based on rotation
   */
  private boolean canSeeBlock(float yaw, float pitch, double blockX, double blockY, double blockZ) {
    // Normalize yaw to -180 to 180
    float normalizedYaw = ((yaw % 360) + 360) % 360;
    if (normalizedYaw > 180) {
      normalizedYaw -= 360;
    }

    // Simple check: pitch should be within reasonable range
    if (pitch < -90 || pitch > 90) {
      return false;
    }

    // This is a simplified check - real implementation would do ray tracing
    // For now, we check if the rotation is within 90 degrees of the block
    return true;
  }

  @Override
  public void onPlayerQuit(String playerId) {
    playerData.remove(playerId);
  }

  private static class GhostHandData {
    volatile float accumulatedYaw;
    volatile float accumulatedPitch;
    volatile boolean hasRotation;
    volatile long lastRotationTime;
    volatile long lastPlaceTime;
    volatile int ghostPlaces;
    volatile int ghostBreaks;
  }
}
