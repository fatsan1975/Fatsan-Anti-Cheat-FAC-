package io.fatsan.fac.service;

import java.util.Locale;

public final class PremiumLicenseService {
  public LicenseValidation validate(String rawKey, boolean premiumEnabled) {
    if (!premiumEnabled) {
      return new LicenseValidation(false, "disabled", "Premium mode disabled in config.");
    }
    if (rawKey == null || rawKey.isBlank()) {
      return new LicenseValidation(false, "missing", "No license key configured.");
    }

    String normalized = rawKey.trim().toUpperCase(Locale.ROOT);
    if (!normalized.matches("FAC-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}")) {
      return new LicenseValidation(false, "invalid_format", "License key format is invalid.");
    }

    int checksum = 0;
    for (char c : normalized.toCharArray()) {
      if (c == '-') {
        continue;
      }
      checksum += c;
    }
    if (checksum % 7 != 0) {
      return new LicenseValidation(false, "checksum_failed", "License key checksum failed.");
    }

    String tier = normalized.charAt(4) >= 'N' ? "enterprise" : "premium";
    return new LicenseValidation(true, tier, "License validated.");
  }

  public record LicenseValidation(boolean valid, String tier, String message) {}
}
