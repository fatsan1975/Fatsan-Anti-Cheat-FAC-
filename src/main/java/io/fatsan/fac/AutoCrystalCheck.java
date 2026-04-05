package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects AutoCrystal — a macro that places and detonates end crystals at
 * machine speed to deal rapid burst damage in PvP.
 *
 * <p>AutoCrystal players place END_CRYSTAL items in extremely rapid succession
 * (often every 50–150ms) with very uniform timing — a pattern impossible for
 * human players.  The placement interval is used as the primary signal.
 *
 * <p>Detection: if a player repeatedly places end crystals with a mean
 * interval well below human reaction time and high cadence uniformity,
 * the pattern is consistent with automation.
 */
public final class AutoCrystalCheck extends AbstractWindowCheck {

  /** Mean placement interval (ns) below which automation is suspected. */
  private static final double MAX_CRYSTAL_INTERVAL_NS = 250_000_000D; // 250ms

  /** Item type key fragment that identifies end crystals. */
  private static final String CRYSTAL_TYPE = "end_crystal";

  public AutoCrystalCheck(int limit) {
    super(limit, 5);
  }

  @Override
  public String name() {
    return "AutoCrystal";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockPlaceEventSignal place) || place.intervalNanos() == Long.MAX_VALUE) {
      return CheckResult.clean(name(), category());
    }

    // Only track end crystal placements
    if (!place.itemTypeKey().toLowerCase(java.util.Locale.ROOT).contains(CRYSTAL_TYPE)) {
      return CheckResult.clean(name(), category());
    }

    var ws = stats.record(place.playerId(), place.intervalNanos());
    if (!ws.hasEnoughData()) {
      return CheckResult.clean(name(), category());
    }

    if (ws.mean() < MAX_CRYSTAL_INTERVAL_NS && ws.isUniformlyCadenced(0.12)) {
      int buf = incrementBuffer(place.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(),
            "AutoCrystal placement (meanInterval=" + String.format("%.0f", ws.mean() / 1_000_000D) + "ms)",
            Math.min(1.0D, buf / 4.0D),
            false);
      }
    } else {
      coolDown(place.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
