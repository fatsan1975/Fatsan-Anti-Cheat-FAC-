package io.fatsan.fac.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PacketTimerServiceTest {
    @Test void normalTimerHasZeroBalance() {
        var service = new PacketTimerService();
        long time = 1_000_000_000L;
        for (int i = 0; i < 20; i++) { time += 50_000_000L; service.onFlyingPacket("p1", time); }
        long balance = service.getBalance("p1");
        assertTrue(Math.abs(balance) < 100_000_000L, "Normal timer balance near zero, got: " + balance);
    }
    @Test void fastTimerAccumulatesPositiveBalance() {
        var service = new PacketTimerService();
        long time = 1_000_000_000L;
        for (int i = 0; i < 100; i++) { time += 48_500_000L; service.onFlyingPacket("p1", time); }
        long balance = service.getBalance("p1");
        assertTrue(balance > 50_000_000L, "Fast timer should have positive balance, got: " + balance);
    }
    @Test void slowTimerAccumulatesNegativeBalance() {
        var service = new PacketTimerService();
        long time = 1_000_000_000L;
        for (int i = 0; i < 100; i++) { time += 51_500_000L; service.onFlyingPacket("p1", time); }
        long balance = service.getBalance("p1");
        assertTrue(balance < -50_000_000L, "Slow timer should have negative balance, got: " + balance);
    }
    @Test void isTimerViolationDetectsOneDotZeroThreeX() {
        var service = new PacketTimerService();
        long time = 1_000_000_000L;
        for (int i = 0; i < 200; i++) { time += 48_500_000L; service.onFlyingPacket("p1", time); }
        assertTrue(service.isTimerViolation("p1"), "1.03x over 200 ticks should be detected");
    }
    @Test void clearPlayerResetsBalance() {
        var service = new PacketTimerService();
        service.onFlyingPacket("p1", 1_000_000_000L);
        service.onFlyingPacket("p1", 1_048_500_000L);
        service.clearPlayer("p1");
        assertEquals(0L, service.getBalance("p1"));
    }
}
