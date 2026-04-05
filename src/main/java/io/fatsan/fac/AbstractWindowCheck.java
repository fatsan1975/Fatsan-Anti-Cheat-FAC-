package io.fatsan.fac.check;

import io.fatsan.fac.check.support.WindowStatsTracker;

/**
 * Base class for checks that combine a {@link WindowStatsTracker} sliding
 * window with the standard {@link AbstractBufferedCheck} escalation buffer.
 *
 * <p>Subclasses get a pre-built {@link #stats} tracker and inherit the
 * buffer increment/cooldown API from {@link AbstractBufferedCheck}.  The
 * only extra responsibility is calling {@code stats.record(...)} (or
 * {@code stats.peek(...)}) in their {@code evaluate(...)} implementation.
 *
 * <p>{@link #onPlayerQuit(String)} is overridden here to clear <em>both</em>
 * the parent buffer state and the window tracker state, preventing unbounded
 * per-player memory growth on busy servers.
 *
 * <p>Usage example:
 * <pre>{@code
 * public final class MyCheck extends AbstractWindowCheck {
 *   public MyCheck(int bufferLimit) {
 *     super(bufferLimit, 8);   // window size 8
 *   }
 *
 *   public CheckResult evaluate(NormalizedEvent event) {
 *     WindowStatsTracker.Stats s = stats.record(playerId, value);
 *     if (!s.hasEnoughData()) return CheckResult.clean(name(), category());
 *     if (s.isUniformlyCadenced(0.03)) { ... }
 *   }
 * }
 * }</pre>
 */
abstract class AbstractWindowCheck extends AbstractBufferedCheck {

  /** Shared window stats tracker.  One instance per check, keyed per player. */
  protected final WindowStatsTracker stats;

  /**
   * @param bufferLimit  the escalation buffer limit passed to
   *                     {@link AbstractBufferedCheck}
   * @param windowSize   the maximum number of observations retained per player
   *                     in the {@link WindowStatsTracker}
   */
  AbstractWindowCheck(int bufferLimit, int windowSize) {
    super(bufferLimit);
    this.stats = new WindowStatsTracker(windowSize);
  }

  /**
   * Clears both the escalation buffer (via the parent) and the window tracker
   * for the given player.
   */
  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    stats.clear(playerId);
  }
}
