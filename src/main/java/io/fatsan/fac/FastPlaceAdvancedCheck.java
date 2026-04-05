package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects fast block placement beyond human input capability.
 *
 * <p><b>Cheat behaviour:</b> "FastPlace" cheats reduce the cooldown between
 * block placement events to 1–10ms.  Vanilla enforces a minimum placement
 * cooldown of one game tick (~50ms).  This check detects placements below
 * the {@value #MIN_INTERVAL_MS}ms threshold consistently.
 *
 * <p>This is a complementary check to {@code PlaceIntervalBurstCheck} and
 * {@code BlockPlaceIntervalConsistencyCheck}.  While those detect statistical
 * patterns, this check provides a hard threshold per-interval combined with
 * sprint-state correlation.
 *
 * <p><b>Sprint correlation:</b> Fast scaffold placement while sprinting is a
 * characteristic of "scaffold" cheats.  We weight the confidence higher when
 * the player is also sprinting ({@code sprinting == true}).
 *
 * <p>Buffer of 3: three consecutive fast placements within a session strongly
 * indicate a cheat rather than a network jitter event.
 */
public final class FastPlaceAdvancedCheck extends AbstractBufferedCheck {

  /** Below this interval (ms) between placements, flag as suspiciously fast. */
  private static final long MIN_INTERVAL_MS = 40L;
  private static final long MIN_INTERVAL_NS = MIN_INTERVAL_MS * 1_000_000L;

  public FastPlaceAdvancedCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "FastPlaceAdvanced";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockPlaceEventSignal place)) {
      return CheckResult.clean(name(), category());
    }

    // First placement — no prior timestamp
    if (place.intervalNanos() == Long.MAX_VALUE || place.intervalNanos() <= 0) {
      return CheckResult.clean(name(), category());
    }

    if (place.intervalNanos() < MIN_INTERVAL_NS) {
      int buf = incrementBuffer(place.playerId());
      if (overLimit(buf)) {
        double confidence = place.sprinting()
            ? Math.min(1.0, buf / 4.0)   // scaffold pattern: higher confidence
            : Math.min(0.7, buf / 5.0);  // stationary fast-place: moderate
        return new CheckResult(
            true,
            name(),
            category(),
            "FastPlace: interval=" + (place.intervalNanos() / 1_000_000)
                + "ms sprint=" + place.sprinting() + " buf=" + buf,
            confidence,
            place.sprinting()); // punish only when combined with sprint (scaffold)
      }
    } else {
      coolDown(place.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
