package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatContextSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects attacks that pass through solid blocks (wallhack / through-wall killaura).
 *
 * <p><b>Cheat behaviour:</b> Wall-hack killaura attacks players through solid
 * blocks.  Vanilla hit logic should reject these server-side, but some cheats
 * exploit lag windows or use reach distances that the server partially accepts.
 * This check provides a secondary layer by checking whether the attack path
 * intersected a solid block.
 *
 * <p><b>Signal:</b> {@link CombatContextSignal#solidBlockBetween()} is pre-
 * computed in {@code BukkitSignalBridge.onHit()} using a DDA (Digital
 * Differential Analyzer) raycast from the attacker's eye position to the
 * victim's centre, at 0.25-block resolution.  This is the only place where
 * {@code World.getBlockAt()} calls are safe on the Folia region thread.
 *
 * <p><b>False-positive mitigations:</b>
 * <ul>
 *   <li>Buffer of 3 — a single false positive (chunk boundary, stale block
 *       data at high ping) will not flag.</li>
 *   <li>Chunk-edge lag can produce false positives when a block is loaded in
 *       one thread while the hit is processed in another.  The buffer absorbs
 *       these transient cases.</li>
 * </ul>
 *
 * <p><b>Folia thread safety:</b> immutable record passed from region thread;
 * buffer state is in {@link AbstractBufferedCheck} via {@code ConcurrentHashMap}.
 */
public final class ThroughWallHitCheck extends AbstractBufferedCheck {

  public ThroughWallHitCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "ThroughWallHit";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatContextSignal ctx)) {
      return CheckResult.clean(name(), category());
    }

    if (ctx.solidBlockBetween()) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Hit through solid block (reach=" + String.format("%.2f", ctx.reachDistance())
                + " buf=" + buf + ")",
            Math.min(1.0, buf / 5.0),
            true);
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
