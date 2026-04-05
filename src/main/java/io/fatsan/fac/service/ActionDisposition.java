package io.fatsan.fac.service;

public enum ActionDisposition {
  ALERT_ONLY,
  REVIEW_ONLY,
  CORROBORATED_SETBACK,
  CORROBORATED_KICK,
  DISABLED_BY_DEFAULT;

  public boolean allowsAlert() {
    return this != DISABLED_BY_DEFAULT;
  }

  public boolean allowsSetback() {
    return this == CORROBORATED_SETBACK || this == CORROBORATED_KICK;
  }

  public boolean allowsKick() {
    return this == CORROBORATED_KICK;
  }

  public boolean requiresCorroboration() {
    return this == CORROBORATED_SETBACK || this == CORROBORATED_KICK;
  }
}
