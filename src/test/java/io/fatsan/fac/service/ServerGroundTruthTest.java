package io.fatsan.fac.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerGroundTruthTest {
    @Test void playerOnSolidBlockIsOnGround() { assertTrue(ServerGroundTruth.isOnGround(65.0, true)); }
    @Test void playerInAirIsNotOnGround() { assertFalse(ServerGroundTruth.isOnGround(65.0, false)); }
    @Test void playerOnBlockEdgeIsOnGround() { assertTrue(ServerGroundTruth.isOnGround(65.001, true)); }
    @Test void playerHighAboveBlockIsNotOnGround() { assertFalse(ServerGroundTruth.isOnGround(65.5, true)); }
    @Test void groundMismatchDetected() {
        var result = ServerGroundTruth.checkMismatch(true, false);
        assertTrue(result.mismatch());
        assertEquals("CLIENT_GROUND_SERVER_AIR", result.type());
    }
    @Test void noMismatchWhenBothAgree() {
        var result = ServerGroundTruth.checkMismatch(true, true);
        assertFalse(result.mismatch());
    }
}
