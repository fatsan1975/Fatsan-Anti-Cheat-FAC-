package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementContextSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects noclip — the player's bounding box occupying a solid block.
 *
 * <p><b>Cheat behaviour:</b> "NoClip" and "phase" cheats allow the player to
 * move through solid blocks.  When active, the player's feet block is solid.
 * The server's collision detection normally prevents this, but some cheats
 * exploit async movement packets or vehicle mode bypasses.
 *
 * <p><b>Signal:</b> {@link MovementContextSignal#insideSolidBlock()} is computed
 * in {@code BukkitSignalBridge.onMove()} as
 * {@code player.getLocation().getBlock().getType().isSolid()}.  This is reliable
 * only on the Folia region thread at event time.
 *
 * <p><b>Note:</b> {@code PhaseCheck} already detects wall-phase via movement
 * delta analysis.  This check is complementary and targets the simpler "feet in
 * block" case that phase detection might miss during slow clip-through.
 *
 * <p>Buffer of 3 — a single false positive (spawn point inside a block during
 * server start, or a teleport that lands inside a block momentarily) won't flag.
 *
 * <p>Exempt: in vehicle (vehicle physics can park inside blocks on some maps).
 */
public final class NoclipCheck extends AbstractBufferedCheck {

  public NoclipCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "Noclip";
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

    if (ctx.inVehicle()) {
      coolDown(ctx.playerId());
      return CheckResult.clean(name(), category());
    }

    if (ctx.insideSolidBlock()) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Player feet inside solid block at ("
                + String.format("%.1f,%.1f,%.1f", ctx.x(), ctx.y(), ctx.z())
                + ") buf=" + buf,
            Math.min(1.0, buf / 4.0),
            true);
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
