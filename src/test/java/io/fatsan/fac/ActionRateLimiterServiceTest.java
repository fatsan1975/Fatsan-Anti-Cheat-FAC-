package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.service.ActionRateLimiterService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ActionRateLimiterServiceTest {
  @Test
  void shouldThrottleAlertsInsideCooldownWindow() {
    AtomicLong now = new AtomicLong(1_000L);
    ActionRateLimiterService service = new ActionRateLimiterService(1_000L, 2_000L, 3_000L, now::get);

    assertTrue(service.allowAlert("player"));
    assertFalse(service.allowAlert("player"));
    now.addAndGet(1_100L);
    assertTrue(service.allowAlert("player"));
  }

  @Test
  void shouldTrackCooldownsIndependentlyByActionType() {
    AtomicLong now = new AtomicLong(5_000L);
    ActionRateLimiterService service = new ActionRateLimiterService(2_000L, 4_000L, 6_000L, now::get);

    assertTrue(service.allowKick("player"));
    assertTrue(service.allowSetback("player"));
    assertFalse(service.allowKick("player"));
    now.addAndGet(4_500L);
    assertFalse(service.allowKick("player"));
    assertTrue(service.allowSetback("player"));
    now.addAndGet(2_000L);
    assertTrue(service.allowKick("player"));
  }

  @Test
  void shouldOnlyGrantOneConcurrentPermitPerCooldownWindow() throws Exception {
    AtomicLong now = new AtomicLong(7_500L);
    ActionRateLimiterService service = new ActionRateLimiterService(5_000L, 5_000L, 5_000L, now::get);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(4);
    try {
      List<Callable<Boolean>> tasks = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
        tasks.add(
            () -> {
              start.await(2, TimeUnit.SECONDS);
              return service.allowAlert("player");
            });
      }

      List<Future<Boolean>> futures = new ArrayList<>();
      for (Callable<Boolean> task : tasks) {
        futures.add(pool.submit(task));
      }
      start.countDown();

      int granted = 0;
      for (Future<Boolean> future : futures) {
        if (future.get(2, TimeUnit.SECONDS)) {
          granted++;
        }
      }
      assertEquals(1, granted);
    } finally {
      pool.shutdownNow();
    }
  }
}
