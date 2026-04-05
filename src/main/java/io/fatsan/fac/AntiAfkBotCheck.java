package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;

/**
 * Detects AFK bots / movement macros by identifying perfectly uniform movement
 * intervals combined with identical deltaXZ values.
 *
 * <p><b>Cheat behaviour:</b> AFK bots typically use a fixed tick-timer or
 * thread sleep to move the player at perfectly regular intervals.  Humans
 * produce irregular movement timing due to input variability.  A bot walking
 * in a straight line or circle produces the same {@code deltaXZ} value every
 * single interval, with variance near zero.
 *
 * <p><b>Detection:</b> We use the window-stats approach via
 * {@link AbstractWindowCheck}: record the last {@value #WINDOW_SIZE}
 * {@code deltaXZ} values.  If their standard deviation is below
 * {@value #VARIANCE_THRESHOLD} (near-zero variance) and all values are
 * above {@value #MIN_MOVING_DELTA} (the player is actually moving), flag.
 *
 * <p>This check uses {@link AbstractWindowCheck} to get both a
 * {@link io.fatsan.fac.check.support.WindowStatsTracker} and the
 * {@link AbstractBufferedCheck} buffer.
 */
public final class AntiAfkBotCheck extends AbstractWindowCheck {

  private static final double MIN_MOVING_DELTA = 0.1;
  private static final double VARIANCE_THRESHOLD = 0.0005;
  private static final int WINDOW_SIZE = 12;

  public AntiAfkBotCheck(int limit) {
    super(limit, WINDOW_SIZE);
  }

  @Override
  public String name() {
    return "AntiAfkBot";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.PROTOCOL;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof PlayerStateEvent state)) {
      return CheckResult.clean(name(), category());
    }

    if (state.onGround() && state.deltaXZ() > MIN_MOVING_DELTA) {
      var s = stats.record(state.playerId(), state.deltaXZ());
      if (!s.hasEnoughData()) return CheckResult.clean(name(), category());

      if (s.variance() < VARIANCE_THRESHOLD && s.mean() > MIN_MOVING_DELTA) {
        int buf = incrementBuffer(state.playerId());
        if (overLimit(buf)) {
          return new CheckResult(
              true,
              name(),
              category(),
              "AFK bot: near-zero movement variance=" + String.format("%.6f", s.variance())
                  + " mean=" + String.format("%.3f", s.mean()) + " buf=" + buf,
              Math.min(0.85, buf / 6.0),
              false); // alert-only: custom travel paths can also be uniform
        }
      } else {
        coolDown(state.playerId());
      }
    } else {
      stats.clear(state.playerId());
      coolDown(state.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
