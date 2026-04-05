package io.fatsan.fac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fatsan.fac.check.RotationQuantizationCheck;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.RotationEvent;
import org.junit.jupiter.api.Test;

final class RotationQuantizationCheckTest {
  @Test
  void flagsRepeatedQuantizedStepsAfterBufferLimit() {
    RotationQuantizationCheck check = new RotationQuantizationCheck(2);
    CheckResult result = CheckResult.clean("RotationQuantization", io.fatsan.fac.model.CheckCategory.COMBAT);

    for (int i = 0; i < 12; i++) {
      float yaw = (i % 2 == 0) ? 3.50F : 3.51F;
      result = check.evaluate(new RotationEvent("player", System.nanoTime(), yaw, 1.0F));
    }

    assertTrue(result.suspicious());
    assertFalse(result.actionable());
  }

  @Test
  void ignoresNaturalVariance() {
    RotationQuantizationCheck check = new RotationQuantizationCheck(2);
    CheckResult result = CheckResult.clean("RotationQuantization", io.fatsan.fac.model.CheckCategory.COMBAT);

    float[] yaws = {2.1F, 4.7F, 3.3F, 5.9F, 2.8F, 6.2F, 4.1F, 3.6F, 5.2F, 2.4F};
    for (float yaw : yaws) {
      result = check.evaluate(new RotationEvent("player", System.nanoTime(), yaw, 0.8F));
    }

    assertFalse(result.suspicious());
  }
}
