package io.fatsan.fac.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PacketContextTest {
    @Test void noopContextReturnsDefaults() {
        var ctx = PacketContext.noop();
        assertFalse(ctx.isPacketEventsAvailable());
        assertTrue(ctx.getServerGround("p1"));
        assertEquals(-1.0, ctx.getLastValidY("p1"));
        assertEquals(-1L, ctx.getTickBalance("p1"));
        assertEquals(-1.0, ctx.getPreciseReach("p1"));
    }
    @Test void liveContextStoresAndRetrievesGround() {
        var ctx = PacketContext.live();
        assertTrue(ctx.isPacketEventsAvailable());
        ctx.setServerGround("p1", false);
        assertFalse(ctx.getServerGround("p1"));
        ctx.setServerGround("p1", true);
        assertTrue(ctx.getServerGround("p1"));
    }
    @Test void liveContextStoresPosition() {
        var ctx = PacketContext.live();
        ctx.setLastValidPosition("p1", 100.0, 65.0, 200.0);
        assertEquals(65.0, ctx.getLastValidY("p1"));
    }
    @Test void liveContextStoresTickBalance() {
        var ctx = PacketContext.live();
        ctx.setTickBalance("p1", 5L);
        assertEquals(5L, ctx.getTickBalance("p1"));
    }
    @Test void liveContextStoresPreciseReach() {
        var ctx = PacketContext.live();
        ctx.setPreciseReach("p1", 3.25);
        assertEquals(3.25, ctx.getPreciseReach("p1"));
    }
    @Test void clearPlayerRemovesAllState() {
        var ctx = PacketContext.live();
        ctx.setServerGround("p1", false);
        ctx.setLastValidPosition("p1", 1, 2, 3);
        ctx.setTickBalance("p1", 10L);
        ctx.setPreciseReach("p1", 3.0);
        ctx.clearPlayer("p1");
        assertTrue(ctx.getServerGround("p1"));
        assertEquals(-1.0, ctx.getLastValidY("p1"));
        assertEquals(-1L, ctx.getTickBalance("p1"));
        assertEquals(-1.0, ctx.getPreciseReach("p1"));
    }
}
