package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects unnaturally constant ground movement speed — a pattern consistent
 * with speed clients that maintain a mechanically precise horizontal velocity
 * without the micro-variations caused by server-side friction, block
 * transitions, and input timing noise.
 *
 * <p>Legitimate sprinting produces subtle speed fluctuations as the engine
 * applies drag, the player adjusts direction, and input timing varies.
 * A speed plateau (very low CV over a sustained window) above vanilla sprint
 * speed is a strong signal of a speed modifier.
 *
 * <p>Uses {@link AbstractWindowCheck} to capture the variance over a window
 * rather than just comparing two consecutive values.
 */
public final class MovementGroundSpeedPlateauCheck extends AbstractWindowCheck {

  private static final double MIN_SUSPICIOUS_SPEED = 0.25;
  private static final double MAX_CV_PLATEAU = 0.018D;


  public MovementGroundSpeedPlateauCheck(int limit) {
    super(limit, 8);
  }

  @Override public String name() { return "MovementGroundSpeedPlateau"; }
  @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent m) || !m.onGround() || m.gliding() || m.inVehicle()) {
      return CheckResult.clean(name(), category());
    }
    var ws = stats.record(m.playerId(), m.deltaXZ());
    if (!ws.hasEnoughData()) return CheckResult.clean(name(), category());

    if (ws.mean() > MIN_SUSPICIOUS_SPEED && ws.isUniformlyCadenced(MAX_CV_PLATEAU)) {
      int buf = incrementBuffer(m.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(),
            "Ground speed plateau sustained over window (mean="
                + String.format("%.3f", ws.mean())
                + " cv=" + String.format("%.4f", ws.entropyScore()) + ")",
            Math.min(1.0D, buf / 7.0D), false);
      }
    } else {
      coolDown(m.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
