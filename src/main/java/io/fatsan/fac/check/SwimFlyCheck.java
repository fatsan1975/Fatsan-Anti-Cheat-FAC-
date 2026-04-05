package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementContextSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects "swim fly" — moving upward through water at abnormal speed.
 *
 * <p><b>Cheat behaviour:</b> "Swim fly" cheats allow moving upward through
 * water without the normal swimming animation or speed constraints.  A player
 * legitimately swimming upward has deltaY ≤ ~0.3 blocks/tick.  Swim-fly
 * can produce deltaY of 1.0+ blocks/tick through water.
 *
 * <p><b>Detection:</b> While {@code inWater == true} and {@code onGround == false},
 * flag if {@code deltaY > threshold} for several consecutive ticks.
 *
 * <p>Exempt: not in water (uses different checks), in vehicle, gliding.
 */
public final class SwimFlyCheck extends AbstractBufferedCheck {

  private static final double UPWARD_THRESHOLD = 0.4;
  private static final int STREAK_THRESHOLD = 3;

  private final Map<String, Integer> streaks = new ConcurrentHashMap<>();

  public SwimFlyCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "SwimFly";
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

    if (!ctx.inWater() || ctx.inVehicle() || ctx.gliding() || ctx.climbable()) {
      streaks.remove(ctx.playerId());
      coolDown(ctx.playerId());
      return CheckResult.clean(name(), category());
    }

    if (ctx.deltaY() > UPWARD_THRESHOLD && !ctx.onGround()) {
      int streak = streaks.merge(ctx.playerId(), 1, Integer::sum);
      if (streak >= STREAK_THRESHOLD) {
        int buf = incrementBuffer(ctx.playerId());
        if (overLimit(buf)) {
          return new CheckResult(
              true,
              name(),
              category(),
              "SwimFly: deltaY=" + String.format("%.3f", ctx.deltaY())
                  + " streak=" + streak + " buf=" + buf,
              Math.min(0.9, buf / 5.0),
              false);
        }
      }
    } else {
      streaks.remove(ctx.playerId());
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    streaks.remove(playerId);
  }
}
