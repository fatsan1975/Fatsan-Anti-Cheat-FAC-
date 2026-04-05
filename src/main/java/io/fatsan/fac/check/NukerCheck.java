package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockBreakContextSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayDeque;

/**
 * Detects "nuker" — breaking multiple blocks in an impossibly short time window.
 *
 * <p><b>Cheat behaviour:</b> "Nuker" cheats break many blocks simultaneously or
 * in rapid succession — far faster than a player can physically swing their arm.
 * In vanilla, the minimum time to break even a very soft block (hardness 0.0,
 * e.g. flowers, leaves) is one game tick (~50ms).  Nuker cheats produce break
 * intervals of 1–10ms.
 *
 * <p><b>Detection:</b> We maintain a rolling time window of the last
 * {@value #WINDOW_SIZE} break events per player.  If more than
 * {@value #MAX_BREAKS_IN_WINDOW} breaks occur within {@value #WINDOW_MS}ms, flag.
 *
 * <p><b>Vanilla baseline:</b> The fastest a human can break soft blocks is
 * ~3 per second (with efficiency + haste).  {@value #MAX_BREAKS_IN_WINDOW}
 * breaks in {@value #WINDOW_MS}ms ≈ 5 per 200ms = 25/s, well above vanilla.
 */
public final class NukerCheck extends AbstractBufferedCheck {

  private static final int WINDOW_SIZE = 10;
  private static final long WINDOW_NS = 200_000_000L; // 200ms
  private static final int MAX_BREAKS_IN_WINDOW = 4;

  private final Map<String, ArrayDeque<Long>> breakTimestamps = new ConcurrentHashMap<>();

  public NukerCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "Nuker";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockBreakContextSignal ctx)) {
      return CheckResult.clean(name(), category());
    }

    ArrayDeque<Long> timestamps = breakTimestamps.computeIfAbsent(
        ctx.playerId(), k -> new ArrayDeque<>(WINDOW_SIZE + 1));

    timestamps.addLast(ctx.nanoTime());

    // Prune events outside the window
    long cutoff = ctx.nanoTime() - WINDOW_NS;
    while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
      timestamps.pollFirst();
    }

    int count = timestamps.size();
    if (count > MAX_BREAKS_IN_WINDOW) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Nuker: " + count + " breaks in 200ms window buf=" + buf,
            Math.min(1.0, count / 8.0),
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
    breakTimestamps.remove(playerId);
  }
}
