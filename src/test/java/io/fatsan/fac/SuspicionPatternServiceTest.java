package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.service.SuspicionPatternService;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class SuspicionPatternServiceTest {
  @Test
  void shouldEscalateMultiplierOnRepeatedSameCategorySuspicion() {
    AtomicLong now = new AtomicLong(1_000L);
    SuspicionPatternService service = new SuspicionPatternService(now::get);
    CheckResult combat =
        new CheckResult(true, "ReachSpikeCluster", CheckCategory.COMBAT, "test", 0.9D, true);

    double m1 = service.onSuspicion("player", combat);
    now.addAndGet(300L);
    double m2 = service.onSuspicion("player", combat);

    assertTrue(m2 > m1);
  }

  @Test
  void shouldDecayMultiplierAfterLongGap() {
    AtomicLong now = new AtomicLong(1_000L);
    SuspicionPatternService service = new SuspicionPatternService(now::get);
    CheckResult protocol =
        new CheckResult(true, "PacketBurst", CheckCategory.PROTOCOL, "test", 0.8D, true);

    double early = service.onSuspicion("player", protocol);
    now.addAndGet(20_000L);
    double afterGap = service.onSuspicion("player", protocol);

    assertTrue(afterGap < early + 0.12D);
  }
}
