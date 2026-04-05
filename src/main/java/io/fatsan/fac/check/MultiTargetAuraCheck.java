package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects multi-target KillAura — attacking two or more different entities
 * within an impossibly short window.
 *
 * <p>Vanilla Minecraft has a 0.5-second attack cooldown per target, and a
 * player's view cannot realistically snap between two targets fast enough to
 * land legitimate hits on both within 250ms.  KillAura with multi-target mode
 * bypasses the cooldown and automatically switches targets without the player
 * moving their camera.
 *
 * <p>Detection: tracks the unique entity IDs hit by each player in a rolling
 * 400ms window.  If two or more distinct entities are hit within that window,
 * the check flags it.
 */
public final class MultiTargetAuraCheck extends AbstractBufferedCheck {

  /** Rolling window duration (nanoseconds) for unique-target tracking. */
  private static final long WINDOW_NANOS = 400_000_000L; // 400ms

  /** Minimum distinct targets within the window to flag. */
  private static final int MIN_DISTINCT_TARGETS = 2;

  private final Map<String, Deque<HitRecord>> recentHits = new ConcurrentHashMap<>();

  public MultiTargetAuraCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "MultiTargetAura";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatHitEvent hit) || hit.targetId().isEmpty()) {
      return CheckResult.clean(name(), category());
    }

    long now = hit.nanoTime();
    Deque<HitRecord> queue = recentHits.computeIfAbsent(hit.playerId(), k -> new ArrayDeque<>());

    synchronized (queue) {
      // Evict old entries
      while (!queue.isEmpty() && now - queue.peekFirst().nanoTime > WINDOW_NANOS) {
        queue.pollFirst();
      }
      queue.addLast(new HitRecord(now, hit.targetId()));

      // Count distinct targets in window
      long distinct = queue.stream().map(HitRecord::targetId).distinct().count();
      if (distinct >= MIN_DISTINCT_TARGETS) {
        int buf = incrementBuffer(hit.playerId());
        if (overLimit(buf)) {
          return new CheckResult(
              true, name(), category(),
              "Multi-target aura (distinctTargets=" + distinct + " window=400ms)",
              Math.min(1.0D, buf / 4.0D),
              false);
        }
      } else {
        coolDown(hit.playerId());
      }
    }

    return CheckResult.clean(name(), category());
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    recentHits.remove(playerId);
  }

  private record HitRecord(long nanoTime, String targetId) {}
}
