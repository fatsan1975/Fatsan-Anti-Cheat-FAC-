package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.service.PacketContext;

/**
 * PE-based precise reach check using eye-to-hitbox distance from PacketContext.
 * Vanilla reach: 3.0 blocks eye-to-hitbox. Threshold: 3.1 (tight, PE-accurate).
 * When PE unavailable, defers to ReachRaycastCheck.
 */
public final class EyeToHitboxReachCheck extends AbstractBufferedCheck {

  private static final double PRECISE_REACH_LIMIT = 3.1;
  private final PacketContext packetContext;

  public EyeToHitboxReachCheck(int limit, PacketContext packetContext) {
    super(limit);
    this.packetContext = packetContext;
  }

  @Override public String name() { return "EyeToHitboxReach"; }
  @Override public CheckCategory category() { return CheckCategory.COMBAT; }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit)) return CheckResult.clean(name(), category());
    if (!packetContext.isPacketEventsAvailable()) return CheckResult.clean(name(), category());

    double preciseReach = packetContext.getPreciseReach(hit.playerId());
    if (preciseReach < 0) return CheckResult.clean(name(), category());

    if (preciseReach > PRECISE_REACH_LIMIT) {
      int buf = incrementBuffer(hit.playerId());
      if (overLimit(buf)) {
        return new CheckResult(true, name(), category(),
            "Precise reach exceeded (eye-to-hitbox=" + String.format("%.2f", preciseReach) + " limit=" + PRECISE_REACH_LIMIT + ")",
            Math.min(1.0, buf / 5.0), true);
      }
    } else {
      coolDown(hit.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
