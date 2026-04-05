package io.fatsan.fac.engine;

import io.fatsan.fac.config.FacConfig;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.EvidenceRecord;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EvidenceService {
  private final int maxWindow;
  private final Map<String, Deque<EvidenceRecord>> byPlayer = new ConcurrentHashMap<>();

  public EvidenceService(FacConfig config) {
    this.maxWindow = config.maxEvidenceWindow();
  }

  public void append(String playerId, CheckResult result) {
    Deque<EvidenceRecord> deque = byPlayer.computeIfAbsent(playerId, key -> new ArrayDeque<>());
    synchronized (deque) {
      if (deque.size() >= maxWindow) {
        deque.pollFirst();
      }
      deque.addLast(new EvidenceRecord(playerId, result.checkName(), result.reason(), result.severity(), System.currentTimeMillis()));
    }
  }

  public List<EvidenceRecord> snapshot(String playerId) {
    Deque<EvidenceRecord> deque = byPlayer.get(playerId);
    if (deque == null) {
      return List.of();
    }
    synchronized (deque) {
      return new ArrayList<>(deque);
    }
  }

  /** Removes all evidence for the given player. Called on disconnect. */
  public void clearPlayer(String playerId) {
    byPlayer.remove(playerId);
  }
}
