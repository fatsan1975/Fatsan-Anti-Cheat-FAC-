package io.fatsan.fac.service;

/**
 * Server-side ground state validation. Compares client-claimed onGround
 * against server block data.
 */
public final class ServerGroundTruth {
  private static final double GROUND_TOLERANCE = 0.03;
  private ServerGroundTruth() {}

  public static boolean isOnGround(double playerY, boolean solidBlockBelow) {
    if (!solidBlockBelow) return false;
    double feetY = playerY - Math.floor(playerY);
    return feetY <= GROUND_TOLERANCE || feetY >= (1.0 - GROUND_TOLERANCE);
  }

  public static MismatchResult checkMismatch(boolean clientOnGround, boolean serverOnGround) {
    if (clientOnGround == serverOnGround) return new MismatchResult(false, "AGREE");
    if (clientOnGround && !serverOnGround) return new MismatchResult(true, "CLIENT_GROUND_SERVER_AIR");
    return new MismatchResult(true, "CLIENT_AIR_SERVER_GROUND");
  }

  public record MismatchResult(boolean mismatch, String type) {}
}
