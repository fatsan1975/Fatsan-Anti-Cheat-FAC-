package io.fatsan.fac.nextlevel;

import io.fatsan.fac.model.NormalizedEvent;

public interface CheckExecutionObserver {
  void onCheckExecution(String checkName, NormalizedEvent event, long latencyNanos, boolean failed);

  CheckExecutionObserver NOOP = (checkName, event, latencyNanos, failed) -> {};
}
