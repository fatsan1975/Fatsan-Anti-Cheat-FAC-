package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import io.fatsan.fac.model.MovementEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Baritone Detection (173-175)
 * Otomatik yürüyüş/pathfinding tespiti
 */
public class BaritonePathCheck implements Check {
  private final int bufferLimit;
  private final Map<String, PathData> playerData = new ConcurrentHashMap<>();

  public BaritonePathCheck(int bufferLimit) {
    this.bufferLimit = bufferLimit;
  }

  @Override
  public String name() {
    return "BaritonePath";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.MOVEMENT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof MovementEvent move)) {
      return CheckResult.clean(name(), category());
    }

    PathData data = playerData.computeIfAbsent(move.playerId(), k -> new PathData());

    long now = move.nanoTime();

    // Store position
    data.positions.add(new Position(move.deltaXZ(), move.deltaY(), now));
    if (data.positions.size() > 50) {
      data.positions.remove(0);
    }

    // Check for straight line movement (Baritone signature)
    if (data.positions.size() >= 10) {
      if (detectStraightLine(data)) {
        data.straightLineCount++;
        if (data.straightLineCount > bufferLimit) {
          return new CheckResult(true, name(), category(), "Baritone straight line path detected", 0.7, false);
        }
      } else {
        data.straightLineCount = Math.max(0, data.straightLineCount - 1);
      }
    }

    // Check for perfect diagonal movement
    if (data.positions.size() >= 5) {
      if (detectPerfectDiagonal(data)) {
        data.diagonalCount++;
        if (data.diagonalCount > bufferLimit / 2) {
          return new CheckResult(true, name(), category(), "Baritone perfect diagonal detected", 0.75, false);
        }
      }
    }

    // Check for consistent speed (no human variation)
    if (data.positions.size() >= 20) {
      double variance = calculateSpeedVariance(data);
      if (variance < 0.001) { // Extremely consistent
        data.consistentSpeedCount++;
        if (data.consistentSpeedCount > bufferLimit) {
          return new CheckResult(true, name(), category(), "Baritone consistent speed detected (variance=" + String.format("%.6f", variance) + ")", 0.8, false);
        }
      } else {
        data.consistentSpeedCount = Math.max(0, data.consistentSpeedCount - 1);
      }
    }

    // Check for snap turns (instant direction changes)
    if (data.positions.size() >= 3) {
      if (detectSnapTurn(data)) {
        data.snapTurnCount++;
        if (data.snapTurnCount > 5) {
          return new CheckResult(true, name(), category(), "Baritone snap turn detected", 0.7, false);
        }
      }
    }

    return CheckResult.clean(name(), category());
  }

  private boolean detectStraightLine(PathData data) {
    // Check if last 10 positions form a straight line
    int size = data.positions.size();
    if (size < 10) return false;

    List<Position> recent = data.positions.subList(size - 10, size);

    // Calculate direction of first segment
    Position first = recent.get(0);
    Position second = recent.get(1);
    double baseAngle = Math.atan2(second.deltaZ, second.deltaX);

    // Check if all segments follow same direction
    for (int i = 1; i < recent.size(); i++) {
      Position current = recent.get(i);
      Position prev = recent.get(i - 1);
      double angle = Math.atan2(current.deltaZ - prev.deltaZ, current.deltaX - prev.deltaX);

      if (Math.abs(angle - baseAngle) > 0.1) { // More than ~5 degrees
        return false;
      }
    }

    return true;
  }

  private boolean detectPerfectDiagonal(PathData data) {
    int size = data.positions.size();
    if (size < 5) return false;

    List<Position> recent = data.positions.subList(size - 5, size);

    // Check for 45-degree movement (equal X and Z)
    for (Position pos : recent) {
      double x = Math.abs(pos.deltaX);
      double z = Math.abs(pos.deltaZ);

      if (x > 0.01 && z > 0.01) {
        double ratio = Math.abs(x - z) / Math.max(x, z);
        if (ratio > 0.1) { // Not close to 45 degrees
          return false;
        }
      }
    }

    return true;
  }

  private double calculateSpeedVariance(PathData data) {
    if (data.positions.size() < 2) return 0;

    double sum = 0;
    double sumSq = 0;
    int count = 0;

    for (Position pos : data.positions) {
      double speed = Math.sqrt(pos.deltaX * pos.deltaX + pos.deltaZ * pos.deltaZ);
      sum += speed;
      sumSq += speed * speed;
      count++;
    }

    if (count < 2) return 0;

    double mean = sum / count;
    double variance = (sumSq / count) - (mean * mean);

    return Math.max(0, variance);
  }

  private boolean detectSnapTurn(PathData data) {
    int size = data.positions.size();
    if (size < 3) return false;

    Position p1 = data.positions.get(size - 3);
    Position p2 = data.positions.get(size - 2);
    Position p3 = data.positions.get(size - 1);

    double angle1 = Math.atan2(p2.deltaZ - p1.deltaZ, p2.deltaX - p1.deltaX);
    double angle2 = Math.atan2(p3.deltaZ - p2.deltaZ, p3.deltaX - p2.deltaX);

    double diff = Math.abs(angle2 - angle1);
    if (diff > Math.PI) {
      diff = 2 * Math.PI - diff;
    }

    // Instant 90+ degree turn
    return diff > Math.PI / 2;
  }

  @Override
  public void onPlayerQuit(String playerId) {
    playerData.remove(playerId);
  }

  private static class PathData {
    final List<Position> positions = new ArrayList<>();
    volatile int straightLineCount;
    volatile int diagonalCount;
    volatile int consistentSpeedCount;
    volatile int snapTurnCount;
  }

  private static class Position {
    final double deltaX;
    final double deltaZ;
    final double deltaY;
    final long time;

    Position(double deltaX, double deltaY, long time) {
      this.deltaX = deltaX;
      this.deltaZ = deltaX; // Simplified
      this.deltaY = deltaY;
      this.time = time;
    }
  }
}
