package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import io.fatsan.fac.service.VelocityTracker;

/**
 * Detects Anti-Knockback — the cheat that negates or significantly reduces
 * the knockback a player receives when hit.
 *
 * <p>When a player is hit in vanilla Minecraft, the server applies a
 * knockback impulse.  The expected horizontal velocity after a hit is
 * approximately 0.4 blocks/tick (8.0 bps) scaled by the attacker's position.
 * A player with Anti-KB will show near-zero horizontal displacement
 * immediately after receiving knockback.
 *
 * <p>Detection uses {@link VelocityTracker} to store the expected post-hit
 * velocity.  On the next {@link PlayerStateEvent} after a hit, the observed
 * horizontal speed is compared against the expectation.  If the player barely
 * moved despite the expected impulse, the check flags it.
 */
public final class AntiKBCheck extends AbstractBufferedCheck {

  /**
   * Ratio threshold: if the observed horizontal speed after a hit is below
   * this fraction of the expected knockback speed, it is suspicious.
   * 0.25 = player absorbed more than 75 % of the knockback.
   */
  private static final double MIN_KB_RATIO = 0.20D;

  /**
   * Minimum expected knockback speed (bps) below which we skip the check
   * (very weak hits, already near-stop players, etc.).
   */
  private static final double MIN_EXPECTED_KB_BPS = 2.0D;

  /** Maximum age (nanoseconds) of a pending knockback expectation. */
  private static final long MAX_KB_AGE_NANOS = 800_000_000L; // 800ms

  private final VelocityTracker velocityTracker;

  public AntiKBCheck(int limit, VelocityTracker velocityTracker) {
    super(limit);
    this.velocityTracker = velocityTracker;
  }

  @Override
  public String name() {
    return "AntiKB";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof PlayerStateEvent state)) {
      return CheckResult.clean(name(), category());
    }

    VelocityTracker.PendingKnockback kb = velocityTracker.consumeKnockback(state.playerId());
    if (kb == null) {
      return CheckResult.clean(name(), category());
    }

    long age = System.nanoTime() - kb.timestamp();
    if (age > MAX_KB_AGE_NANOS) {
      return CheckResult.clean(name(), category());
    }

    double expectedBps = kb.expectedHorizontal() * 20.0; // ticks → bps
    if (expectedBps < MIN_EXPECTED_KB_BPS) {
      return CheckResult.clean(name(), category());
    }

    double seconds = state.intervalNanos() / 1_000_000_000.0D;
    if (seconds <= 0.0D || seconds > 0.2D) {
      return CheckResult.clean(name(), category());
    }

    double observedBps = state.deltaXZ() / seconds;
    double ratio = expectedBps > 0 ? observedBps / expectedBps : 1.0;

    if (ratio < MIN_KB_RATIO) {
      int buf = incrementBuffer(state.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Knockback absorption: observed="
                + String.format("%.1f", observedBps)
                + "bps expected="
                + String.format("%.1f", expectedBps)
                + "bps ratio="
                + String.format("%.2f", ratio),
            Math.min(1.0D, buf / 5.0D),
            false);
      }
    } else {
      coolDown(state.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
