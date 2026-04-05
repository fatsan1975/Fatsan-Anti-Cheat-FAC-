package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementContextSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects freecam position desync — a teleport-sized position delta that
 * was not preceded by a server-side teleport event.
 *
 * <p><b>Cheat behaviour:</b> "Freecam" cheats let the player's client spectate
 * from a detached camera position.  Some implementations work by sending the
 * player's real position to the server only when the player manually "confirms"
 * their new position, causing a sudden large delta.  The position jump is
 * too large to be legitimate movement in one tick.
 *
 * <p><b>Detection:</b> We track the previous absolute position and flag when
 * the Euclidean distance to the current position exceeds
 * {@value #TELEPORT_SIZE_BLOCKS} blocks in one movement event, where neither
 * deltaXZ nor deltaY individually exceeds the threshold (it is a 3D jump, not
 * a long-jump in one axis).
 *
 * <p><b>False-positive mitigations:</b>
 * <ul>
 *   <li>Server teleports legitimately produce large deltas; BukkitSignalBridge
 *       should reset the previous-position tracker on teleport events.</li>
 *   <li>Buffer of 2 — two consecutive desync events within a short window
 *       strongly indicate cheating.</li>
 * </ul>
 */
public final class FreecamDesyncCheck extends AbstractBufferedCheck {

  private static final double TELEPORT_SIZE_BLOCKS = 8.0;

  private final Map<String, double[]> prevPos = new ConcurrentHashMap<>();

  public FreecamDesyncCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "FreecamDesync";
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

    double[] prev = prevPos.get(ctx.playerId());
    prevPos.put(ctx.playerId(), new double[]{ctx.x(), ctx.y(), ctx.z()});

    if (prev == null) {
      return CheckResult.clean(name(), category());
    }

    double dx = ctx.x() - prev[0];
    double dy = ctx.y() - prev[1];
    double dz = ctx.z() - prev[2];
    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

    if (dist > TELEPORT_SIZE_BLOCKS) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Freecam desync: position jump=" + String.format("%.1f", dist)
                + " blocks buf=" + buf,
            Math.min(1.0, dist / 20.0),
            false); // alert-only: server teleports can produce this
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    prevPos.remove(playerId);
  }
}
