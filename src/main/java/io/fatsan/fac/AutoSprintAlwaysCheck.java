package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.PlayerStateEvent;

/**
 * Detects "sprint always" / "toggle sprint" cheats that keep sprinting
 * continuously even during actions that should cancel sprint.
 *
 * <p><b>Cheat behaviour:</b> In vanilla Minecraft, sprinting is cancelled when:
 * (a) the player opens their inventory, (b) the player eats/drinks/blocks,
 * (c) the player takes damage and their food bar drops below a threshold.
 * "Sprint always" modules bypass this by re-enabling sprint every tick,
 * so the player is always sprinting even while eating or blocking.
 *
 * <p><b>Detection:</b> Flag when {@code sprinting == true} while
 * {@code eating == true} OR {@code blocking == true}.  Vanilla: sprint is
 * cancelled on the same tick that an item use action starts.  A player that
 * is simultaneously sprinting and using an item is violating this invariant.
 *
 * <p>Requires 3 consecutive violations to flag (a single lag spike can cause
 * a missed cancel packet in Via translation layers).
 */
public final class AutoSprintAlwaysCheck extends AbstractBufferedCheck {

  public AutoSprintAlwaysCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "AutoSprintAlways";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof PlayerStateEvent state)) {
      return CheckResult.clean(name(), category());
    }

    // Sprint should be cancelled while eating or blocking
    if (state.sprinting() && (state.eating() || state.blocking())) {
      int buf = incrementBuffer(state.playerId());
      if (overLimit(buf)) {
        String action = state.eating() ? "eating" : "blocking";
        return new CheckResult(
            true,
            name(),
            category(),
            "Sprint while " + action + " (buf=" + buf + ")",
            Math.min(0.9, buf / 5.0),
            false); // alert-only: Via clients may delay cancel packet
      }
    } else {
      coolDown(state.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
