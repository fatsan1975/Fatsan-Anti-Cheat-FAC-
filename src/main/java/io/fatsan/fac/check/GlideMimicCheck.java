package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects fake elytra glide — clients that set the gliding flag while not
 * actually wearing an elytra, or manipulate elytra physics to produce
 * impossible flight patterns.
 *
 * <p>Genuine elytra gliding follows specific physics:
 * <ul>
 *   <li>Minimum forward speed is required to maintain altitude</li>
 *   <li>Horizontal speed and vertical speed are coupled (glide ratio ~10:1)</li>
 *   <li>A player cannot gain vertical altitude while gliding horizontally
 *       at normal speed (rocket boosted flight excepted)</li>
 * </ul>
 *
 * <p>This check flags sustained upward altitude gain while the gliding flag
 * is set without the expected horizontal speed to justify altitude recovery
 * (firework rocket threshold).  It also flags the combination of extremely
 * high horizontal speed with upward movement that violates the glide ratio.
 */
public final class GlideMimicCheck extends AbstractWindowCheck {

  /**
   * Maximum sustained upward velocity (blocks/second) while gliding without
   * a firework rocket.  Fireworks give ~0.5 block/tick upward; normal gliding
   * is always descending or near-horizontal.
   */
  private static final double MAX_GLIDE_UPWARD_BPS = 4.0D;

  /**
   * Minimum horizontal speed (blocks/second) required to maintain altitude
   * while gliding.  Below this speed, elytra physics pull the player down.
   */
  private static final double MIN_GLIDE_SUSTAIN_SPEED_BPS = 7.0D;

  /** Maximum interval seconds accepted for speed computation. */
  private static final double MAX_INTERVAL_SECONDS = 0.15D;

  public GlideMimicCheck(int limit) {
    super(limit, 6);
  }

  @Override
  public String name() {
    return "GlideMimic";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement) || !movement.gliding()) {
      return CheckResult.clean(name(), category());
    }

    if (movement.inVehicle() || movement.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    double seconds = movement.intervalNanos() / 1_000_000_000.0D;
    if (seconds <= 0.0D || seconds > MAX_INTERVAL_SECONDS) {
      return CheckResult.clean(name(), category());
    }

    double speedBps = movement.deltaXZ() / seconds;
    double vertBps = movement.deltaY() / seconds;

    // Record upward velocity while gliding
    var ws = stats.record(movement.playerId(), vertBps);
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    // Flag: sustained upward flight while gliding with too little horizontal speed
    boolean impossibleAscent = ws.mean() > MAX_GLIDE_UPWARD_BPS
        && speedBps < MIN_GLIDE_SUSTAIN_SPEED_BPS;

    if (impossibleAscent) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Impossible elytra ascent (vertMean="
                + String.format("%.1f", ws.mean())
                + "bps speed="
                + String.format("%.1f", speedBps)
                + "bps)",
            Math.min(1.0D, buf / 5.0D),
            false);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
