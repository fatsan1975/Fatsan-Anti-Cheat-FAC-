package io.fatsan.fac.service;

import io.fatsan.fac.model.CheckResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class PremiumInsightsService {
  private final AtomicLong totalAlerts = new AtomicLong();
  private final AtomicLong totalSeverity = new AtomicLong();
  private final Map<String, AtomicLong> alertsByCheck = new ConcurrentHashMap<>();

  public void record(CheckResult result) {
    totalAlerts.incrementAndGet();
    totalSeverity.addAndGet(Math.max(1, Math.round(result.severity())));
    alertsByCheck.computeIfAbsent(result.checkName(), key -> new AtomicLong()).incrementAndGet();
  }

  public PremiumSnapshot snapshot() {
    long alerts = totalAlerts.get();
    double avgSeverity = alerts == 0 ? 0.0D : ((double) totalSeverity.get()) / alerts;
    List<String> topChecks =
        alertsByCheck.entrySet().stream()
            .sorted(Comparator.comparingLong((Map.Entry<String, AtomicLong> e) -> e.getValue().get()).reversed())
            .limit(3)
            .map(entry -> entry.getKey() + "=" + entry.getValue().get())
            .toList();
    return new PremiumSnapshot(alerts, avgSeverity, topChecks);
  }

  public record PremiumSnapshot(long totalAlerts, double averageSeverity, List<String> topChecks) {}
}
