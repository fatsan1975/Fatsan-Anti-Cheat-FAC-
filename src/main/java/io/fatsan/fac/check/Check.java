package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

public interface Check {
  String name();

  CheckCategory category();

  CheckResult evaluate(NormalizedEvent event);

  /**
   * Called when a player disconnects.  Implementations should release any
   * per-player state (buffers, window trackers, streak counters) to prevent
   * unbounded memory growth on busy servers.
   *
   * <p>The default no-op implementation is safe for checks that do not
   * maintain mutable per-player state.
   */
  default void onPlayerQuit(String playerId) {}
}
