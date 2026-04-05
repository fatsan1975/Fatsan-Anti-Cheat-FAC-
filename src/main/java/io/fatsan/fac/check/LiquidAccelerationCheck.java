package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementContextSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects liquid acceleration — moving faster than vanilla allows inside
 * water or lava.
 *
 * <p><b>Cheat behaviour:</b> "Water speed" and "lava walk" cheats remove the
 * drag applied in fluids.  Vanilla water speed is ~2.1 bps (swimming), lava
 * ~1.3 bps.  Cheats can bring this up to normal sprint speed or higher.
 *
 * <p><b>Vanilla liquid speed limits (approximate):</b>
 * <ul>
 *   <li>Water swimming: ~2.1 bps (Dolphin's Grace potion: ~5.0 bps)</li>
 *   <li>Lava: ~1.3 bps</li>
 * </ul>
 *
 * <p>Threshold of {@value #WATER_THRESHOLD_BPS} bps for water (above Dolphin's
 * Grace) and {@value #LAVA_THRESHOLD_BPS} bps for lava.
 *
 * <p>Interval guard applied to skip lag-spike events.
 */
public final class LiquidAccelerationCheck extends AbstractBufferedCheck {

  private static final double WATER_THRESHOLD_BPS = 7.0; // above Dolphin's Grace
  private static final double LAVA_THRESHOLD_BPS = 3.0;
  private static final long INTERVAL_GUARD_NS = 150_000_000L;

  public LiquidAccelerationCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "LiquidAcceleration";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementContextSignal ctx)) {
      return CheckResult.clean(name(), category());
    }

    if (ctx.inVehicle() || ctx.gliding()
        || ctx.intervalNanos() > INTERVAL_GUARD_NS
        || ctx.horizontalSpeedBps() <= 0) {
      return CheckResult.clean(name(), category());
    }

    double threshold;
    String fluidName;

    if (ctx.inWater()) {
      threshold = WATER_THRESHOLD_BPS;
      fluidName = "water";
    } else if (ctx.inLava()) {
      threshold = LAVA_THRESHOLD_BPS;
      fluidName = "lava";
    } else {
      coolDown(ctx.playerId());
      return CheckResult.clean(name(), category());
    }

    if (ctx.horizontalSpeedBps() > threshold) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Liquid acceleration in " + fluidName + ": speed="
                + String.format("%.1f", ctx.horizontalSpeedBps()) + " bps buf=" + buf,
            Math.min(1.0, ctx.horizontalSpeedBps() / (threshold * 2)),
            false); // alert: Dolphin's Grace edge cases exist
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
