package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementContextSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects "dash" or "blink" cheats that produce sustained abnormally high
 * horizontal speed over multiple ticks.
 *
 * <p><b>Cheat behaviour:</b> Unlike {@code LongJumpCheck} which catches a
 * single huge delta, "dash" cheats maintain a constant high speed over many
 * ticks.  The {@link MovementContextSignal#horizontalSpeedBps()} field gives
 * blocks-per-second, making it easy to compare against known vanilla maxima.
 *
 * <p><b>Vanilla speed reference (blocks/second):</b>
 * <ul>
 *   <li>Walking: ~4.3 bps</li>
 *   <li>Sprinting: ~5.6 bps</li>
 *   <li>Sprint + Speed I: ~7.1 bps</li>
 *   <li>Sprint + Speed II: ~8.6 bps</li>
 *   <li>Ice + sprint: ~25+ bps (legitimate)</li>
 * </ul>
 *
 * <p>The threshold of {@value #THRESHOLD_BPS} bps is set high enough to exclude
 * ice or boat movement, while flagging true speed hacks.
 *
 * <p>Exempt: in vehicle, gliding, in water/lava (ice checks use SpeedEnvelope).
 */
public final class DashHackCheck extends AbstractBufferedCheck {

  /** Threshold above which sustained speed is flagged (bps). */
  private static final double THRESHOLD_BPS = 30.0;
  private static final long INTERVAL_GUARD_NS = 150_000_000L;

  public DashHackCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "DashHack";
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

    if (ctx.inVehicle() || ctx.gliding() || ctx.inWater() || ctx.inLava()) {
      coolDown(ctx.playerId());
      return CheckResult.clean(name(), category());
    }

    // horizontalSpeedBps is 0 when interval exceeds 200ms (lag guard in BukkitSignalBridge)
    if (ctx.intervalNanos() > INTERVAL_GUARD_NS || ctx.horizontalSpeedBps() <= 0) {
      return CheckResult.clean(name(), category());
    }

    if (ctx.horizontalSpeedBps() > THRESHOLD_BPS) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Dash hack: speed=" + String.format("%.1f", ctx.horizontalSpeedBps())
                + " bps buf=" + buf,
            Math.min(1.0, ctx.horizontalSpeedBps() / 60.0),
            true);
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
