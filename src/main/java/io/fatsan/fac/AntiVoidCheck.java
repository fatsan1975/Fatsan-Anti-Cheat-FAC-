package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementContextSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects "anti-void" / void-walk cheats that prevent the player from falling
 * into the void.
 *
 * <p><b>Cheat behaviour:</b> When falling into the void (Y below build limit),
 * some cheats detect the void threshold and instantly reverse vertical velocity
 * (producing a positive deltaY from a very negative one).  This is visible as
 * a deltaY reversal below Y = 0 without touching ground.
 *
 * <p><b>Detection:</b>
 * <ul>
 *   <li>Track the previous {@code deltaY} and {@code y} position.</li>
 *   <li>Flag when: {@code y < 0}, {@code !onGround}, and the deltaY reversed
 *       from strongly negative (&lt; -0.5) to positive (&gt; 0.1) in one tick
 *       without ground contact.</li>
 * </ul>
 *
 * <p>Buffer of 2 — two void reversals within a session are nearly impossible
 * for a legitimate player.
 */
public final class AntiVoidCheck extends AbstractBufferedCheck {

  private static final double VOID_Y_THRESHOLD = 0.0;
  private static final double FALLING_FAST_THRESHOLD = -0.5;
  private static final double REVERSAL_THRESHOLD = 0.1;

  private final Map<String, Double> prevDeltaY = new ConcurrentHashMap<>();

  public AntiVoidCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "AntiVoid";
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

    double lastDeltaY = prevDeltaY.getOrDefault(ctx.playerId(), 0.0);
    prevDeltaY.put(ctx.playerId(), ctx.deltaY());

    if (ctx.y() < VOID_Y_THRESHOLD
        && !ctx.onGround()
        && !ctx.inVehicle()
        && lastDeltaY < FALLING_FAST_THRESHOLD
        && ctx.deltaY() > REVERSAL_THRESHOLD) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Anti-void: Y=" + String.format("%.1f", ctx.y())
                + " prevDeltaY=" + String.format("%.2f", lastDeltaY)
                + " curDeltaY=" + String.format("%.2f", ctx.deltaY())
                + " buf=" + buf,
            Math.min(1.0, buf / 3.0),
            true);
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    prevDeltaY.remove(playerId);
  }
}
