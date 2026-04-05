package io.fatsan.fac.model;

public record CheckResult(
    boolean suspicious,
    String checkName,
    CheckCategory category,
    String reason,
    double severity,
    boolean actionable) {
  public static CheckResult clean(String checkName, CheckCategory category) {
    return new CheckResult(false, checkName, category, "", 0.0D, false);
  }
}
