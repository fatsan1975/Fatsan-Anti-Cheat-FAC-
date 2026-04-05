package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that AbstractWindowCheck correctly clears both the parent buffer
 * state and the WindowStatsTracker on player quit.
 */
class AbstractWindowCheckTest {

  /** Minimal concrete subclass for testing. */
  private static final class TestWindowCheck extends AbstractWindowCheck {
    TestWindowCheck() {
      super(5, 8);
    }

    @Override public String name() { return "TestWindow"; }
    @Override public CheckCategory category() { return CheckCategory.MOVEMENT; }

    @Override
    public CheckResult evaluate(NormalizedEvent event) {
      return CheckResult.clean(name(), category());
    }

    /** Expose buffer for white-box testing. */
    int bufferValue(String playerId) {
      return incrementBuffer(playerId) - 1; // increment then read, then undo would be complex — just count
    }

    /** Record a value and return the stats window size. */
    int windowSize(String playerId, double value) {
      return stats.record(playerId, value).windowSize();
    }

    /** Peek window size without adding a value. */
    int peekWindowSize(String playerId) {
      return stats.peek(playerId).windowSize();
    }
  }

  @Test
  void onPlayerQuitClearsWindowTracker() {
    TestWindowCheck check = new TestWindowCheck();
    String player = "player-uuid-1";

    // Populate the window
    for (int i = 0; i < 5; i++) {
      check.windowSize(player, i * 10.0);
    }
    assertTrue(check.peekWindowSize(player) >= 3);

    // Quit should clear the window
    check.onPlayerQuit(player);

    assertEquals(0, check.peekWindowSize(player), "Window tracker should be empty after quit");
  }

  @Test
  void onPlayerQuitClearsBuffer() {
    TestWindowCheck check = new TestWindowCheck();
    String player = "player-uuid-2";

    // Increment buffer several times
    check.incrementBuffer(player);
    check.incrementBuffer(player);
    check.incrementBuffer(player);

    // Quit should clear the buffer (buffer will restart at 0)
    check.onPlayerQuit(player);

    // After quit, the next increment should return 1 (clean start)
    int firstAfterQuit = check.incrementBuffer(player);
    assertEquals(1, firstAfterQuit, "Buffer should restart after quit");
  }

  @Test
  void onPlayerQuitIsolatedBetweenPlayers() {
    TestWindowCheck check = new TestWindowCheck();
    String p1 = "player-uuid-3";
    String p2 = "player-uuid-4";

    for (int i = 0; i < 5; i++) {
      check.windowSize(p1, i * 5.0);
      check.windowSize(p2, i * 5.0);
    }

    // Only quit p1
    check.onPlayerQuit(p1);

    assertEquals(0, check.peekWindowSize(p1), "p1 window should be cleared");
    assertTrue(check.peekWindowSize(p2) >= 3, "p2 window should remain intact");
  }

  @Test
  void onPlayerQuitSafeForUnknownPlayer() {
    TestWindowCheck check = new TestWindowCheck();
    // Should not throw for a player with no state
    assertDoesNotThrow(() -> check.onPlayerQuit("unknown-player-uuid"));
  }
}
