package io.fatsan.fac.prediction;

import org.bukkit.util.Vector;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Movement Prediction Engine (41-60)
 * Grim'in prediction motoru - Client hareket simülasyonu
 */
public class MovementPredictionEngine {
  private static final Logger LOGGER = Logger.getLogger(MovementPredictionEngine.class.getName());

  // Vanilla movement constants
  private static final double DEFAULT_FRICTION = 0.98;
  private static final double AIR_FRICTION = 0.98;
  private static final double SPRINT_MULTIPLIER = 1.3;
  private static final double SNEAK_MULTIPLIER = 0.3;
  private static final double WALK_SPEED = 0.1;
  private static final double SPRINT_SPEED = 0.13;
  private static final double JUMP_FORCE = 0.42;
  private static final double GRAVITY = 0.08;
  private static final double DRAG = 0.98;

  private final Map<UUID, PredictedState> predictedStates = new ConcurrentHashMap<>();
  private final Map<UUID, PlayerMovementData> playerData = new ConcurrentHashMap<>();

  /**
   * Vanilla Movement Simulator (41)
   * Simulates vanilla client movement
   */
  public PredictedState simulateMovement(UUID playerId, MovementInput input, long deltaNanos) {
    PlayerMovementData data = playerData.computeIfAbsent(playerId, k -> new PlayerMovementData());
    PredictedState state = predictedStates.computeIfAbsent(playerId, k -> new PredictedState());

    double deltaTicks = deltaNanos / 50_000_000.0; // Convert to ticks (50ms per tick)

    // Apply sprint momentum (45)
    double speed = input.sprinting ? SPRINT_SPEED : WALK_SPEED;
    if (input.sneaking) {
      speed *= SNEAK_MULTIPLIER;
    }

    // Apply potion effects (46-49)
    speed = applyPotionEffects(speed, input);

    // Calculate movement delta
    Vector movement = new Vector(input.forward, 0, input.strafe).normalize().multiply(speed);

    // Apply friction (54)
    if (input.onGround) {
      movement.multiply(DEFAULT_FRICTION);
    } else {
      movement.multiply(AIR_FRICTION);
    }

    // Apply gravity (59)
    if (!input.onGround) {
      state.velocityY -= GRAVITY * deltaTicks;
      state.velocityY *= DRAG;
    } else if (input.jumping) {
      state.velocityY = JUMP_FORCE;
    } else {
      state.velocityY = 0;
    }

    // Update position
    state.x += movement.getX() * deltaTicks;
    state.y += state.velocityY * deltaTicks;
    state.z += movement.getZ() * deltaTicks;

    // Ground check
    state.onGround = input.onGround;

    // Store prediction
    data.lastPredictedX = state.x;
    data.lastPredictedY = state.y;
    data.lastPredictedZ = state.z;

    return state;
  }

  /**
   * Client-Side Prediction Engine (42)
   * Predicts client movement based on inputs
   */
  public PredictedState predictClientMovement(UUID playerId, long lookAheadNanos) {
    PlayerMovementData data = playerData.get(playerId);
    if (data == null) {
      return new PredictedState();
    }

    PredictedState current = predictedStates.getOrDefault(playerId, new PredictedState());
    PredictedState predicted = new PredictedState();

    // Copy current state
    predicted.x = current.x;
    predicted.y = current.y;
    predicted.z = current.z;
    predicted.velocityY = current.velocityY;
    predicted.onGround = current.onGround;

    // Predict forward
    double deltaTicks = lookAheadNanos / 50_000_000.0;

    // Apply current velocity
    predicted.x += data.lastDeltaX * deltaTicks;
    predicted.y += data.lastDeltaY * deltaTicks;
    predicted.z += data.lastDeltaZ * deltaTicks;

    return predicted;
  }

  /**
   * Position Reconciliation System (43)
   * Reconciles server position with predicted position
   */
  public ReconciliationResult reconcilePosition(UUID playerId, double actualX, double actualY, double actualZ) {
    PredictedState predicted = predictedStates.get(playerId);
    PlayerMovementData data = playerData.get(playerId);

    if (predicted == null || data == null) {
      return new ReconciliationResult(0, 0, 0, true);
    }

    double deltaX = actualX - predicted.x;
    double deltaY = actualY - predicted.y;
    double deltaZ = actualZ - predicted.z;

    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

    // Update deltas for next prediction
    data.lastDeltaX = actualX - data.lastX;
    data.lastDeltaY = actualY - data.lastY;
    data.lastDeltaZ = actualZ - data.lastZ;
    data.lastX = actualX;
    data.lastY = actualY;
    data.lastZ = actualZ;

    // Threshold for desync (44)
    boolean inSync = distance < 0.03; // 3cm tolerance

    if (!inSync) {
      // Update prediction to actual
      predicted.x = actualX;
      predicted.y = actualY;
      predicted.z = actualZ;
    }

    return new ReconciliationResult(deltaX, deltaY, deltaZ, inSync);
  }

  /**
   * Movement Delta Validator (44)
   */
  public boolean validateMovementDelta(UUID playerId, double deltaX, double deltaY, double deltaZ, long deltaNanos) {
    PlayerMovementData data = playerData.get(playerId);
    if (data == null) {
      return true;
    }

    double deltaTicks = deltaNanos / 50_000_000.0;
    double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) / deltaTicks;

    // Maximum reasonable speed (with sprint + speed potion)
    double maxSpeed = SPRINT_SPEED * 2.5; // Allow for speed potions

    return speed <= maxSpeed;
  }

  /**
   * Sprint Momentum Calculator (45)
   */
  public double calculateSprintMomentum(UUID playerId, boolean sprinting, double currentSpeed) {
    if (!sprinting) {
      return currentSpeed;
    }
    return Math.min(currentSpeed * SPRINT_MULTIPLIER, SPRINT_SPEED * 1.5);
  }

  /**
   * Potion Effect Validators (46-49)
   */
  private double applyPotionEffects(double speed, MovementInput input) {
    // Speed potion
    if (input.speedAmplifier > 0) {
      speed *= (1.0 + 0.2 * input.speedAmplifier);
    }

    // Slowness
    if (input.slownessAmplifier > 0) {
      speed *= (1.0 - 0.15 * input.slownessAmplifier);
    }

    return speed;
  }

  public boolean validateJumpBoost(UUID playerId, int amplifier, double jumpHeight) {
    double expectedHeight = JUMP_FORCE * (1.0 + 0.5 * amplifier);
    return Math.abs(jumpHeight - expectedHeight) < 0.1;
  }

  public boolean validateLevitation(UUID playerId, int amplifier, double verticalVelocity) {
    double expectedVelocity = 0.05 * (amplifier + 1);
    return Math.abs(verticalVelocity - expectedVelocity) < 0.02;
  }

  /**
   * Dolphin's Grace Validator (51)
   */
  public boolean validateDolphinsGrace(UUID playerId, double swimSpeed) {
    double maxDolphinSpeed = 0.15; // Significantly faster swimming
    return swimSpeed <= maxDolphinSpeed;
  }

  /**
   * Depth Strider Validator (52)
   */
  public boolean validateDepthStrider(UUID playerId, int level, double waterSpeed) {
    double maxSpeed = WALK_SPEED * (1.0 + level * 0.33);
    return waterSpeed <= maxSpeed;
  }

  /**
   * Soul Speed Validator (53)
   */
  public boolean validateSoulSpeed(UUID playerId, int level, double speed) {
    double maxSoulSpeed = WALK_SPEED * (1.0 + level * 0.105);
    return speed <= maxSoulSpeed;
  }

  /**
   * Knockback Simulation (54)
   */
  public Vector simulateKnockback(UUID playerId, Vector attackerLocation, double strength) {
    PlayerMovementData data = playerData.get(playerId);
    if (data == null) {
      return new Vector(0, 0, 0);
    }

    Vector playerPos = new Vector(data.lastX, data.lastY, data.lastZ);
    Vector knockback = playerPos.subtract(attackerLocation).normalize().multiply(strength);
    knockback.setY(0.4); // Upward component

    return knockback;
  }

  /**
   * Explosion Knockback Validator (55)
   */
  public boolean validateExplosionKnockback(UUID playerId, Vector explosionCenter, double power, Vector actualVelocity) {
    PlayerMovementData data = playerData.get(playerId);
    if (data == null) {
      return true;
    }

    Vector playerPos = new Vector(data.lastX, data.lastY, data.lastZ);
    double distance = playerPos.distance(explosionCenter);

    if (distance > power * 2) {
      return true; // Out of range
    }

    double force = (1.0 - distance / (power * 2)) * power;
    Vector expectedVelocity = playerPos.subtract(explosionCenter).normalize().multiply(force);
    expectedVelocity.setY(force * 0.5);

    double diff = actualVelocity.distance(expectedVelocity);
    return diff < 0.5;
  }

  /**
   * Entity Collision Prediction (56)
   */
  public boolean predictEntityCollision(UUID playerId, Vector entityLocation, double entityRadius) {
    PredictedState state = predictedStates.get(playerId);
    if (state == null) {
      return false;
    }

    Vector playerPos = new Vector(state.x, state.y, state.z);
    double distance = playerPos.distance(entityLocation);

    return distance < (0.6 + entityRadius); // Player radius ~0.6
  }

  /**
   * Block Collision Prediction (57)
   */
  public boolean predictBlockCollision(UUID playerId, double blockX, double blockY, double blockZ) {
    PredictedState state = predictedStates.get(playerId);
    if (state == null) {
      return false;
    }

    // Simple AABB check
    double playerMinX = state.x - 0.3;
    double playerMaxX = state.x + 0.3;
    double playerMinY = state.y;
    double playerMaxY = state.y + 1.8;
    double playerMinZ = state.z - 0.3;
    double playerMaxZ = state.z + 0.3;

    return (playerMinX < blockX + 1 && playerMaxX > blockX &&
        playerMinY < blockY + 1 && playerMaxY > blockY &&
        playerMinZ < blockZ + 1 && playerMaxZ > blockZ);
  }

  /**
   * Liquid Physics Simulation (58)
   */
  public Vector simulateLiquidMovement(UUID playerId, boolean inWater, boolean inLava, Vector input) {
    double friction = inWater ? 0.8 : (inLava ? 0.5 : DEFAULT_FRICTION);
    double speed = inWater ? 0.02 : (inLava ? 0.02 : WALK_SPEED);

    Vector movement = input.normalize().multiply(speed);
    movement.multiply(friction);

    // Apply buoyancy
    if (inWater || inLava) {
      movement.setY(0.04); // Slight upward drift
    }

    return movement;
  }

  /**
   * Cobweb Physics Simulation (59)
   */
  public Vector simulateCobwebMovement(UUID playerId, Vector velocity) {
    // Cobwebs drastically reduce movement
    return velocity.multiply(0.05);
  }

  /**
   * Climbable Physics Simulation (60)
   */
  public Vector simulateClimbableMovement(UUID playerId, boolean climbing, boolean ascending) {
    Vector movement = new Vector(0, 0, 0);

    if (climbing) {
      if (ascending) {
        movement.setY(0.12); // Climb up
      } else {
        movement.setY(-0.12); // Climb down
      }
    }

    return movement;
  }

  /**
   * Update player state from actual position
   */
  public void updateActualPosition(UUID playerId, double x, double y, double z, boolean onGround) {
    PredictedState state = predictedStates.computeIfAbsent(playerId, k -> new PredictedState());
    state.x = x;
    state.y = y;
    state.z = z;
    state.onGround = onGround;

    PlayerMovementData data = playerData.computeIfAbsent(playerId, k -> new PlayerMovementData());
    data.lastX = x;
    data.lastY = y;
    data.lastZ = z;
  }

  /**
   * Clear player data on disconnect
   */
  public void clearPlayer(UUID playerId) {
    predictedStates.remove(playerId);
    playerData.remove(playerId);
  }

  // Data classes
  public static class MovementInput {
    public double forward;
    public double strafe;
    public boolean jumping;
    public boolean sprinting;
    public boolean sneaking;
    public boolean onGround;
    public int speedAmplifier;
    public int slownessAmplifier;
    public int jumpBoostAmplifier;
  }

  public static class PredictedState {
    public double x, y, z;
    public double velocityY;
    public boolean onGround;
    public long timestamp;
  }

  public static class PlayerMovementData {
    public double lastX, lastY, lastZ;
    public double lastPredictedX, lastPredictedY, lastPredictedZ;
    public double lastDeltaX, lastDeltaY, lastDeltaZ;
    public boolean wasOnGround;
    public long lastUpdateNanos;
  }

  public static class ReconciliationResult {
    public final double deltaX, deltaY, deltaZ;
    public final boolean inSync;

    public ReconciliationResult(double deltaX, double deltaY, double deltaZ, boolean inSync) {
      this.deltaX = deltaX;
      this.deltaY = deltaY;
      this.deltaZ = deltaZ;
      this.inSync = inSync;
    }
  }
}
