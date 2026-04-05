package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.MovementFoliaRegionIoFuseCheck;
import io.fatsan.fac.model.MovementEvent;
import org.junit.jupiter.api.Test;

class MovementFoliaRegionIoFuseCheckTest {
  @Test
  void shouldFlagWhenSlowTickComesWithLargeGroundDisplacement() {
    MovementFoliaRegionIoFuseCheck check = new MovementFoliaRegionIoFuseCheck(2);
    MovementEvent event = new MovementEvent("p", 1L, 0.53D, 0.0D, true, 0.0F, false, false, 100_000_000L);
    check.evaluate(event);
    assertTrue(check.evaluate(event).suspicious());
  }
}
