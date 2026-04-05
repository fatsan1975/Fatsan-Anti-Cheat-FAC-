package io.fatsan.fac.service;

public enum SuspicionTier {
  BASELINE,
  ELEVATED,
  HOT;

  public boolean atLeast(SuspicionTier other) {
    return this.ordinal() >= other.ordinal();
  }
}
