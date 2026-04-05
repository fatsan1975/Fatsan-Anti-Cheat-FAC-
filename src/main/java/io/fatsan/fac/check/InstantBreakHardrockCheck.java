package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockBreakContextSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects instant-breaking of hard blocks that cannot be broken quickly in vanilla.
 *
 * <p><b>Cheat behaviour:</b> "InstantBreak" cheats send a block-break packet
 * immediately after starting to break, without waiting for the dig animation
 * to complete.  This can bypass the server-side break-time calculation on some
 * implementations, especially with custom tools or when the server trusts the
 * client's break completion.
 *
 * <p><b>Detection:</b> We check if a block with hardness ≥ {@value #HARDNESS_THRESHOLD}
 * was broken in less than {@value #MIN_BREAK_NS} nanoseconds
 * ({@value #MIN_BREAK_MS}ms).  The minimum vanilla break time for a block of
 * hardness 30 with a maximally enchanted tool is approximately 0.8 seconds.
 *
 * <p><b>Hardness reference:</b>
 * <ul>
 *   <li>Stone: 1.5</li>
 *   <li>Obsidian: 50.0</li>
 *   <li>Crying Obsidian: 50.0</li>
 *   <li>Ender Chest: 22.5</li>
 *   <li>Diamond ore: 3.0</li>
 * </ul>
 *
 * <p>Note: This check uses {@code intervalNanos} from the signal, which is the
 * time since the previous block break by the same player, not the break duration
 * for this specific block.  It is therefore a rate check for hard blocks, not
 * an exact duration check.
 */
public final class InstantBreakHardrockCheck extends AbstractBufferedCheck {

  private static final float HARDNESS_THRESHOLD = 25.0f;
  private static final long MIN_BREAK_NS = 800_000_000L;  // 800ms
  private static final int MIN_BREAK_MS = 800;

  public InstantBreakHardrockCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "InstantBreakHardrock";
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

    if (ctx.blockHardness() < HARDNESS_THRESHOLD) {
      coolDown(ctx.playerId());
      return CheckResult.clean(name(), category());
    }

    // First break for this player — no previous timestamp yet
    if (ctx.intervalNanos() == Long.MAX_VALUE || ctx.intervalNanos() <= 0) {
      return CheckResult.clean(name(), category());
    }

    if (ctx.intervalNanos() < MIN_BREAK_NS) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "InstantBreak hard block: " + ctx.blockMaterialName()
                + " (hardness=" + ctx.blockHardness()
                + " interval=" + (ctx.intervalNanos() / 1_000_000) + "ms"
                + " min=" + MIN_BREAK_MS + "ms buf=" + buf + ")",
            Math.min(1.0, buf / 4.0),
            true);
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
