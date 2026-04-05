package io.fatsan.fac.transaction;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transaction-Based Sync System (26-40)
 * Grim'in en güçlü özelliği - Client-server senkronizasyonu
 */
public class TransactionManager {
  private static final Logger LOGGER = Logger.getLogger(TransactionManager.class.getName());
  private static final int MAX_TRANSACTION_QUEUE_SIZE = 64;
  private static final long TRANSACTION_TIMEOUT_NANOS = 5_000_000_000L; // 5 seconds

  // Transaction ID tracker (26)
  private final AtomicInteger globalTransactionId = new AtomicInteger(0);

  // Per-player transaction tracking
  private final Map<UUID, PlayerTransactionState> playerStates = new ConcurrentHashMap<>();

  // Transaction acknowledgment system (27)
  private final Map<UUID, Queue<PendingTransaction>> pendingTransactions = new ConcurrentHashMap<>();

  // KeepAlive tracking
  private final Map<UUID, Map<Long, Long>> pendingKeepAlives = new ConcurrentHashMap<>();

  /**
   * Register a new transaction (server -> client)
   */
  public void registerTransaction(UUID playerId, int windowId, int actionId, long nanoTime) {
    PlayerTransactionState state = playerStates.computeIfAbsent(playerId, k -> new PlayerTransactionState());

    int transactionId = globalTransactionId.incrementAndGet();
    state.lastTransactionId = transactionId;
    state.lastTransactionTime = nanoTime;

    Queue<PendingTransaction> queue = pendingTransactions.computeIfAbsent(playerId, k -> new LinkedList<>());

    // Queue management (30)
    if (queue.size() >= MAX_TRANSACTION_QUEUE_SIZE) {
      queue.poll(); // Remove oldest
    }

    queue.offer(new PendingTransaction(transactionId, windowId, actionId, nanoTime));

    // Latency-compensated transaction (31)
    state.transactionCounter++;
  }

  /**
   * Register KeepAlive (server -> client)
   */
  public void registerKeepAlive(UUID playerId, long id, long nanoTime) {
    Map<Long, Long> keepAlives = pendingKeepAlives.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
    keepAlives.put(id, nanoTime);

    // Cleanup old keepalives
    if (keepAlives.size() > 20) {
      keepAlives.entrySet().removeIf(e -> nanoTime - e.getValue() > TRANSACTION_TIMEOUT_NANOS);
    }
  }

  /**
   * Handle KeepAlive response (client -> server)
   */
  public void handleKeepAliveResponse(UUID playerId, long id, long nanoTime) {
    Map<Long, Long> keepAlives = pendingKeepAlives.get(playerId);
    if (keepAlives != null) {
      Long sentTime = keepAlives.remove(id);
      if (sentTime != null) {
        long latency = nanoTime - sentTime;
        PlayerTransactionState state = playerStates.get(playerId);
        if (state != null) {
          state.lastPing = latency / 1_000_000; // Convert to ms
          // Exponential moving average
          state.smoothedPing = (state.smoothedPing * 0.8) + (state.lastPing * 0.2);
        }
      }
    }
  }

  /**
   * Handle Pong (transaction acknowledgment)
   */
  public void handlePong(UUID playerId, int id, long nanoTime) {
    Queue<PendingTransaction> queue = pendingTransactions.get(playerId);
    if (queue != null) {
      queue.removeIf(t -> t.id == id);
    }

    PlayerTransactionState state = playerStates.get(playerId);
    if (state != null) {
      state.acknowledgedTransactions++;
    }
  }

  /**
   * Client-server state reconciliation (28)
   */
  public TransactionState getTransactionState(UUID playerId) {
    PlayerTransactionState state = playerStates.get(playerId);
    if (state == null) {
      return TransactionState.UNKNOWN;
    }

    Queue<PendingTransaction> queue = pendingTransactions.get(playerId);
    if (queue == null || queue.isEmpty()) {
      return TransactionState.SYNCED;
    }

    long now = System.nanoTime();
    long oldestTransaction = queue.peek().timestamp;

    if (now - oldestTransaction > TRANSACTION_TIMEOUT_NANOS) {
      return TransactionState.DESYNCED;
    }

    return TransactionState.PENDING;
  }

  /**
   * Transaction timeout handling (29)
   */
  public void checkTimeouts() {
    long now = System.nanoTime();

    for (Map.Entry<UUID, Queue<PendingTransaction>> entry : pendingTransactions.entrySet()) {
      Queue<PendingTransaction> queue = entry.getValue();
      queue.removeIf(t -> now - t.timestamp > TRANSACTION_TIMEOUT_NANOS);
    }
  }

  /**
   * Get pending transaction count for player
   */
  public int getPendingTransactionCount(UUID playerId) {
    Queue<PendingTransaction> queue = pendingTransactions.get(playerId);
    return queue != null ? queue.size() : 0;
  }

  /**
   * Get smoothed ping for player
   */
  public long getSmoothedPing(UUID playerId) {
    PlayerTransactionState state = playerStates.get(playerId);
    return state != null ? (long) state.smoothedPing : 0;
  }

  /**
   * Clear player data on disconnect
   */
  public void clearPlayer(UUID playerId) {
    playerStates.remove(playerId);
    pendingTransactions.remove(playerId);
    pendingKeepAlives.remove(playerId);
  }

  /**
   * Transaction rollback mechanism (36)
   */
  public void rollbackTransactions(UUID playerId, int count) {
    Queue<PendingTransaction> queue = pendingTransactions.get(playerId);
    if (queue != null) {
      for (int i = 0; i < count && !queue.isEmpty(); i++) {
        queue.poll();
      }
    }
  }

  /**
   * Transaction prediction engine (37)
   */
  public long predictTransactionArrival(UUID playerId, int transactionId) {
    PlayerTransactionState state = playerStates.get(playerId);
    if (state == null) {
      return System.nanoTime();
    }

    // Predict based on smoothed ping
    long pingNanos = (long) (state.smoothedPing * 1_000_000);
    return System.nanoTime() + pingNanos;
  }

  // Transaction-based validation methods (32-35)
  public boolean isTransactionSynced(UUID playerId) {
    return getTransactionState(playerId) == TransactionState.SYNCED;
  }

  public boolean hasPendingTransactions(UUID playerId) {
    Queue<PendingTransaction> queue = pendingTransactions.get(playerId);
    return queue != null && !queue.isEmpty();
  }

  public void markDesync(UUID playerId) {
    PlayerTransactionState state = playerStates.computeIfAbsent(playerId, k -> new PlayerTransactionState());
    state.desyncCount++;
  }

  public void markResync(UUID playerId) {
    PlayerTransactionState state = playerStates.get(playerId);
    if (state != null) {
      state.desyncCount = 0;
    }
  }

  /**
   * Transaction resync protocol (40)
   */
  public void requestResync(UUID playerId) {
    Queue<PendingTransaction> queue = pendingTransactions.get(playerId);
    if (queue != null) {
      queue.clear();
    }
    markDesync(playerId);
  }

  // Inner classes
  private static class PlayerTransactionState {
    volatile int lastTransactionId;
    volatile long lastTransactionTime;
    volatile int transactionCounter;
    volatile int acknowledgedTransactions;
    volatile long lastPing;
    volatile double smoothedPing = 50.0;
    volatile int desyncCount;
  }

  private static class PendingTransaction {
    final int id;
    final int windowId;
    final int actionId;
    final long timestamp;

    PendingTransaction(int id, int windowId, int actionId, long timestamp) {
      this.id = id;
      this.windowId = windowId;
      this.actionId = actionId;
      this.timestamp = timestamp;
    }
  }

  public enum TransactionState {
    UNKNOWN,
    SYNCED,
    PENDING,
    DESYNCED
  }
}
