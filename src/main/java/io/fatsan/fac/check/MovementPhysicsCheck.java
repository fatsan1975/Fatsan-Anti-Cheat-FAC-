package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.service.MovementPhysicsValidator;

/**
 * Physics-based movement validator that detects flight and speed hacks by
 * comparing observed movement against vanilla physics predictions.
 *
 * <p>This check maintains a per-player physics simulation.  On each movement
 * event it predicts where the player should be based on the previous tick's
 * velocity, then compares the prediction against the observed displacement.
 * Significant and sustained deviations from the physics model indicate
 * server-side movement manipulation.
 *
 * <p>Precision note: Bukkit events do not always fire on every client tick,
 * so all thresholds include generous tolerances.  The goal is to catch obvious
 * cheats (constant flight, extreme speed) without producing false positives
 * from server lag, ViaVersion timing, or legitimate speed effects.
 */
public final class MovementPhysicsCheck extends AbstractWindowCheck {

  /**
   * Maximum ratio of observed-to-expected horizontal speed before the
   * window mean is considered suspicious.  1.6 = 60 % above physics max.
   */
  private static final double MAX_SPEED_RATIO = 1.6D;

  /**
   * Maximum sustained upward velocity (bps) while airborne that physics
   * can explain.  JUMP_VELOCITY * 20 ticks + tolerance.
   */
  private static final double MAX_SUSTAINED_UPWARD_BPS = (MovementPhysicsValidator.MAX_JUMP_VELOCITY * 20.0) * 1.3;

  /** Minimum interval seconds to compute a valid speed measurement. */
  private static final double MIN_INTERVAL_SECONDS = 0.02D;
  private static final double MAX_INTERVAL_SECONDS = 0.15D;

  private final MovementPhysicsValidator physicsValidator;

  public MovementPhysicsCheck(int limit, MovementPhysicsValidator physicsValidator) {
    super(limit, 6);
    this.physicsValidator = physicsValidator;
  }

  @Override
  public String name() {
    return "MovementPhysics";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement) || movement.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    if (movement.gliding() || movement.inVehicle()) {
      physicsValidator.clearPlayer(movement.playerId());
      coolDown(movement.playerId());
      return CheckResult.clean(name(), category());
    }

    double seconds = movement.intervalNanos() / 1_000_000_000.0D;
    if (seconds < MIN_INTERVAL_SECONDS || seconds > MAX_INTERVAL_SECONDS) {
      return CheckResult.clean(name(), category());
    }

    double speedBps = movement.deltaXZ() / seconds;
    double maxExpectedBps = MovementPhysicsValidator.predictMaxHorizontalBps(true, -1, -1);

    // Update physics state
    physicsValidator.update(
        movement.playerId(),
        movement.deltaXZ(),
        movement.deltaY(),
        movement.onGround(),
        movement.intervalNanos());

    // Record speed ratio in window
    double ratio = speedBps / maxExpectedBps;
    var ws = stats.record(movement.playerId(), ratio);

    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    // Check 1: sustained horizontal overspeed
    if (ws.mean() > MAX_SPEED_RATIO) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Physics overspeed: speed="
                + String.format("%.1f", speedBps)
                + "bps max="
                + String.format("%.1f", maxExpectedBps)
                + "bps ratio="
                + String.format("%.2f", ws.mean()),
            Math.min(1.0D, buf / 7.0D),
            true);
      }
      return CheckResult.clean(name(), category());
    }

    // Check 2: sustained upward flight while not on ground and not jumping naturally
    double vertBps = movement.deltaY() / seconds;
    if (!movement.onGround() && vertBps > MAX_SUSTAINED_UPWARD_BPS) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Physics upward flight: vertSpeed="
                + String.format("%.1f", vertBps)
                + "bps maxJump="
                + String.format("%.1f", MAX_SUSTAINED_UPWARD_BPS)
                + "bps",
            Math.min(1.0D, buf / 6.0D),
            true);
      }
      return CheckResult.clean(name(), category());
    }

    coolDown(movement.playerId());
    return CheckResult.clean(name(), category());
  }
}
