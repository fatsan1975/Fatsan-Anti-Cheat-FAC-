package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.service.PremiumLicenseService;
import org.junit.jupiter.api.Test;

class PremiumLicenseServiceTest {
  @Test
  void shouldRejectWhenPremiumDisabled() {
    PremiumLicenseService service = new PremiumLicenseService();

    PremiumLicenseService.LicenseValidation validation = service.validate("FAC-FAAA-AAAA-AAAA", false);

    assertFalse(validation.valid());
  }

  @Test
  void shouldRejectInvalidChecksumKey() {
    PremiumLicenseService service = new PremiumLicenseService();

    PremiumLicenseService.LicenseValidation validation = service.validate("FAC-AAAA-AAAA-AAAA", true);

    assertFalse(validation.valid());
  }

  @Test
  void shouldAcceptValidChecksumKey() {
    PremiumLicenseService service = new PremiumLicenseService();

    PremiumLicenseService.LicenseValidation validation = service.validate("FAC-FAAA-AAAA-AAAA", true);

    assertTrue(validation.valid());
  }
}
