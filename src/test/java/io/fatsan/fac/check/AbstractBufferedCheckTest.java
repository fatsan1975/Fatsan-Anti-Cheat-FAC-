package io.fatsan.fac.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AbstractBufferedCheckTest {
  @Test
  void shouldPassivelyDecayBeforeNextIncrement() {
    AtomicLong now = new AtomicLong(0L);
    TestBufferedCheck check = new TestBufferedCheck(now);

    assertEquals(1, check.bump("player"));
    assertEquals(2, check.bump("player"));
    now.set(1_000_000_000L); // ~3 passive decay steps
    int value = check.bump("player");

    assertTrue(value < 3);
  }

  private static final class TestBufferedCheck extends AbstractBufferedCheck {
    private final AtomicLong now;

    private TestBufferedCheck(AtomicLong now) {
      super(6);
      this.now = now;
    }

    private int bump(String playerId) {
      return incrementBuffer(playerId);
    }

    @Override
    protected long nowNanos() {
      return now.get();
    }

    @Override
    public String name() {
      return "TestBuffered";
    }

    @Override
    public CheckCategory category() {
      return CheckCategory.PROTOCOL;
    }

    @Override
    public CheckResult evaluate(NormalizedEvent event) {
      return CheckResult.clean(name(), category());
    }
  }
}
