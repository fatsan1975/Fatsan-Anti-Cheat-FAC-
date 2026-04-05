package io.fatsan.fac.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiscordWebhookServiceTest {
    @Test void buildEmbedJsonIsValidFormat() {
        var json = DiscordWebhookService.buildEmbedJson("TestPlayer", "SpeedEnvelope", 0.85, 7.5, "Speed exceeded", "#ff4444");
        assertTrue(json.contains("\"username\":\"FAC Anti-Cheat\""));
        assertTrue(json.contains("\"title\":\"SpeedEnvelope\""));
        assertTrue(json.contains("TestPlayer"));
        assertTrue(json.contains("0.85"));
        assertTrue(json.contains("7.50"));
    }
    @Test void shouldSendRespectsMinRisk() {
        var service = new DiscordWebhookService("", 6.0, 10, true);
        assertTrue(service.shouldSend(7.0));
        assertFalse(service.shouldSend(3.0));
    }
    @Test void rateLimitingPreventsSpam() {
        var service = new DiscordWebhookService("", 0.0, 2, true);
        assertTrue(service.tryAcquireRateLimit());
        assertTrue(service.tryAcquireRateLimit());
        assertFalse(service.tryAcquireRateLimit());
    }
    @Test void disabledServiceNeverSends() {
        var service = new DiscordWebhookService("", 0.0, 10, false);
        assertFalse(service.shouldSend(100.0));
    }
}
