package io.fatsan.fac.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AlertWebhookService {
  private final Logger logger;
  private final boolean enabled;
  private final String url;
  private final HttpClient httpClient;

  public AlertWebhookService(Logger logger, boolean enabled, String url) {
    this(logger, enabled, url, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
  }

  AlertWebhookService(Logger logger, boolean enabled, String url, HttpClient httpClient) {
    this.logger = logger;
    this.enabled = enabled;
    this.url = url;
    this.httpClient = httpClient;
  }

  public void publish(String message) {
    if (!enabled || url == null || url.isBlank()) {
      return;
    }

    String payload = "{\"content\":\"" + escapeJson(message) + "\"}";
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .timeout(Duration.ofSeconds(3))
            .build();

    httpClient
        .sendAsync(request, HttpResponse.BodyHandlers.discarding())
        .whenComplete(
            (response, throwable) -> {
              if (throwable != null) {
                logger.log(Level.FINE, "FAC webhook send failed", throwable);
                return;
              }
              if (response.statusCode() >= 300) {
                logger.log(Level.FINE, "FAC webhook rejected status=" + response.statusCode());
              }
            });
  }

  private static String escapeJson(String message) {
    return message.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
