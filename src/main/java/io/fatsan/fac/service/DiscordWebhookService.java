package io.fatsan.fac.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends Discord webhook notifications for anti-cheat alerts.
 * Rate-limited. Async on virtual threads.
 */
public final class DiscordWebhookService {
  private static final Logger LOGGER = Logger.getLogger(DiscordWebhookService.class.getName());
  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5)).build();

  private final String webhookUrl;
  private final double minRisk;
  private final int maxPerMinute;
  private final boolean enabled;
  private final AtomicInteger sentThisMinute = new AtomicInteger(0);
  private volatile long currentMinuteStart = System.currentTimeMillis();
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  public DiscordWebhookService(String webhookUrl, double minRisk, int maxPerMinute, boolean enabled) {
    this.webhookUrl = webhookUrl;
    this.minRisk = minRisk;
    this.maxPerMinute = maxPerMinute;
    this.enabled = enabled;
  }

  public boolean shouldSend(double risk) { return enabled && risk >= minRisk; }

  public boolean tryAcquireRateLimit() {
    long now = System.currentTimeMillis();
    if (now - currentMinuteStart > 60_000) { currentMinuteStart = now; sentThisMinute.set(0); }
    return sentThisMinute.incrementAndGet() <= maxPerMinute;
  }

  public void sendAlert(String playerId, String checkName, double severity, double risk, String reason) {
    if (!shouldSend(risk) || !tryAcquireRateLimit()) return;
    if (webhookUrl == null || webhookUrl.isBlank()) return;
    String json = buildEmbedJson(playerId, checkName, severity, risk, reason, "#ff4444");
    executor.submit(() -> {
      try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
      } catch (Exception e) { LOGGER.log(Level.WARNING, "Failed to send Discord webhook", e); }
    });
  }

  static String buildEmbedJson(String playerId, String checkName, double severity,
      double risk, String reason, String color) {
    return """
        {"username":"FAC Anti-Cheat","embeds":[{"title":"%s","color":%d,"fields":[{"name":"Player","value":"%s","inline":true},{"name":"Severity","value":"%.2f","inline":true},{"name":"Risk","value":"%.2f","inline":true},{"name":"Reason","value":"%s"}]}]}"""
        .formatted(escapeJson(checkName), parseColor(color), escapeJson(playerId),
            severity, risk, escapeJson(reason));
  }

  private static int parseColor(String hex) {
    try { return Integer.parseInt(hex.replace("#", ""), 16); }
    catch (Exception e) { return 0xff4444; }
  }

  private static String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }

  public void shutdown() { executor.shutdownNow(); }
}
