package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import io.fatsan.fac.service.VelocityTracker;

/**
 * Detects velocity manipulation — cheats that modify the player's velocity
 * vector directly (BoatFly, velocity-based fly hacks, or custom velocity
 * injection via packet modification).
 *
 * <p>Velocity manipulation is detected by comparing the player's actual
 * velocity vector (as seen by the server) against the physically plausible
 * range.  A velocity that exceeds vanilla physics maximums without a
 * corresponding legitimate cause (explosion, elytra boost, etc.) is flagged.
 *
 * <p>The {@link VelocityTracker} provides the last recorded velocity.  This
 * check examines the current velocity snapshot from the {@link PlayerStateEvent}
 * and flags when the horizontal or vertical component is physically impossible.
 */
public final class VelocityManipulationCheck extends AbstractBufferedCheck {

  /**
   * Maximum legitimate server-applied horizontal velocity (blocks/tick).
   * TNT/creeper explosions can push ~2.0 bpt; this threshold is generous.
   */
  private static final double MAX_HORIZONTAL_VELOCITY = 3.0D;

  /**
   * Maximum legitimate upward vertical velocity (blocks/tick).
   * Jump = 0.42, slime = ~1.0, riptide = ~2.0.
   */
  private static final double MAX_UPWARD_VELOCITY = 2.5D;

  /** Minimum horizontal velocity considered worth checking (noise floor). */
  private static final double MIN_VELOCITY_NOISE_FLOOR = 0.5D;

  private final VelocityTracker velocityTracker;

  public VelocityManipulationCheck(int limit, VelocityTracker velocityTracker) {
    super(limit);
    this.velocityTracker = velocityTracker;
  }

  @Override
  public String name() {
    return "VelocityManipulation";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof PlayerStateEvent state)) {
      return CheckResult.clean(name(), category());
    }

    if (state.gliding() || state.inVehicle()) {
      return CheckResult.clean(name(), category());
    }

    // Update velocity tracker with current server-observed velocity
    velocityTracker.recordVelocity(state.playerId(), state.velocityX(), state.velocityY(), state.velocityZ());

    double horizontalV = Math.sqrt(state.velocityX() * state.velocityX() + state.velocityZ() * state.velocityZ());
    double verticalV = state.velocityY();

    if (horizontalV < MIN_VELOCITY_NOISE_FLOOR) {
      return CheckResult.clean(name(), category());
    }

    boolean horizontalExcess = horizontalV > MAX_HORIZONTAL_VELOCITY;
    boolean verticalExcess = verticalV > MAX_UPWARD_VELOCITY;

    if (horizontalExcess || verticalExcess) {
      int buf = incrementBuffer(state.playerId());
      if (overLimit(buf)) {
        String axis = horizontalExcess ? "horizontal=" + String.format("%.2f", horizontalV) + "bpt"
            : "vertical=" + String.format("%.2f", verticalV) + "bpt";
        return new CheckResult(
            true,
            name(),
            category(),
            "Velocity exceeds physics envelope (" + axis + ")",
            Math.min(1.0D, buf / 5.0D),
            false);
      }
    } else {
      coolDown(state.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
