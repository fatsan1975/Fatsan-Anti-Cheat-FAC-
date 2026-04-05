package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.service.PacketContext;
import io.fatsan.fac.service.ServerGroundTruth;

/**
 * Detects ground spoof bypass by comparing client-claimed onGround with
 * server-side ground truth from PacketContext.
 * Only flags CLIENT_GROUND_SERVER_AIR (the dangerous spoof direction).
 * When PacketEvents is unavailable, never flags (noop context returns true).
 */
public final class GroundTruthMismatchCheck extends AbstractBufferedCheck {

  private final PacketContext packetContext;

  public GroundTruthMismatchCheck(int limit, PacketContext packetContext) {
    super(limit);
    this.packetContext = packetContext;
  }

  @Override
  public String name() {
    return "GroundTruthMismatch";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent movement)) {
      return CheckResult.clean(name(), category());
    }

    boolean clientGround = movement.onGround();
    boolean serverGround = packetContext.getServerGround(movement.playerId());

    ServerGroundTruth.MismatchResult mismatch =
        ServerGroundTruth.checkMismatch(clientGround, serverGround);

    // Only flag the dangerous direction: client says ground, server says air
    if (mismatch.mismatch() && "CLIENT_GROUND_SERVER_AIR".equals(mismatch.type())) {
      int buf = incrementBuffer(movement.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true, name(), category(),
            "Ground spoof: client=ground server=air",
            Math.min(1.0, buf / 6.0),
            true);
      }
    } else {
      coolDown(movement.playerId());
    }

    return CheckResult.clean(name(), category());
  }
}
