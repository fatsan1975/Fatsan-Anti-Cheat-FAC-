# CHANGELOG_ITERATION_6

## Tarih
2026-03-19

## Özet
Iteration 6, Iteration 5'te başlayan memory-leak kapatma işini tamamladı.
Check-level temizlemenin yanı sıra tüm runtime servisler artık player disconnect'te
kendi per-player state'lerini siliyor.

---

## 1. Servis clearPlayer zinciri — tam tamamlandı

### Değişen servisler

| Servis | Eklenen Metod | Temizlenen State |
|---|---|---|
| `RiskService` | `clearPlayer(String)` | `riskByPlayer` map entry |
| `PlayerTrustService` | `clearPlayer(String)` | `trustByPlayer` map entry |
| `SuspicionPatternService` | `clearPlayer(String)` | `states` map entry |
| `CorroborationService` | `clearPlayer(String)` | `entriesByPlayer` map entry |
| `EvidenceService` | `clearPlayer(String)` | `byPlayer` map entry |

### AntiCheatEngine koordinatörü

```java
public void clearPlayer(String playerId) {
    riskService.clearPlayer(playerId);
    playerTrustService.clearPlayer(playerId);
    suspicionPatternService.clearPlayer(playerId);
    corroborationService.clearPlayer(playerId);
    evidenceService.clearPlayer(playerId);
}
```

`EvidenceService` artık `AntiCheatEngine` constructor parametresi olarak alınıyor
ve `clearPlayer` zincirinde yer alıyor.

### BukkitSignalBridge.onQuit — tam zincir

```java
public void onQuit(PlayerQuitEvent event) {
    String playerId = event.getPlayer().getUniqueId().toString();
    tracker.clear(uuid);               // interval tracker
    playerStateService.clear(uuid);    // safe location
    intake.registry().clearPlayer(playerId);  // check buffers + window trackers
    engine.clearPlayer(playerId);      // risk + trust + suspicion + corroboration + evidence
}
```

`BukkitSignalBridge` artık constructor'da `AntiCheatEngine` referansı alıyor.

---

## 2. Hangi state ÖNCEKİ durumda temizlenmiyordu?

Iteration 5 öncesinde disconnect eden oyuncuların aşağıdaki state'leri hafızada kalıyordu:

| Sınıf | Leak riski |
|---|---|
| `RiskService.riskByPlayer` | Her oyuncu için ~1 `Double` — yüksek turnover'da birikirdi |
| `PlayerTrustService.trustByPlayer` | Aynı |
| `SuspicionPatternService.states` | `State` nesnesi (~3 field) + map entry |
| `CorroborationService.entriesByPlayer` | Bounded deque, ancak map key kalırdı |
| `EvidenceService.byPlayer` | Bounded deque (~96 `EvidenceRecord`), önemli boyut |
| `AbstractBufferedCheck.buffers` | Her check için 1 `BufferState` per player |
| `WindowStatsTracker.windows` | `ArrayDeque<Double>` per player (8-10 entry) |

**Toplam tahmini: ~200+ nesne leak, 182 check × per-player state ile çarpılınca
yüksek oyuncu geçişli sunucularda (hub, minigame) belirgin hafıza basıncı yaratırdı.**

---

## 3. FatsanAntiCheatPlugin — constructor güncelleme

`AntiCheatEngine` constructor'ına `evidenceService` parametresi eklendi.
`BukkitSignalBridge` constructor'ına `engine` parametresi eklendi.

---

## 4. Test coverage

**Yeni test:** `PlayerQuitCleanupTest.java` (11 senaryo)

| Test | Kapsadığı alan |
|---|---|
| `riskService_clearPlayerResetsRisk` | Risk sıfırlanıyor |
| `riskService_clearSafeForUnknownPlayer` | Exception yok |
| `riskService_clearDoesNotAffectOtherPlayers` | Izolasyon |
| `trustService_clearPlayerResetsToInitial` | Trust sıfırlanıyor |
| `trustService_clearSafeForUnknownPlayer` | Exception yok |
| `suspicionPattern_clearPlayerResetsIntensity` | Intensity sıfırlanıyor |
| `suspicionPattern_clearSafeForUnknownPlayer` | Exception yok |
| `corroboration_clearPlayerRemovesEvidence` | Corroboration state temizleniyor |
| `corroboration_clearSafeForUnknownPlayer` | Exception yok |
| `corroboration_clearIsolated` | Diğer oyuncuya etki yok |
| *(via AbstractWindowCheckTest)* | Check buffer + window tracker temizleniyor |

---

## Sonraki adımlar (Iteration 7 önerileri)

1. `survival` profil davranışını `ActionPolicyService`'te gerçekten farklılaştırma
2. Minified check'lerin okunabilir formata getirilmesi (CombatHitDistancePlateauCheck vb.)
3. `ProtocolProfileResolver` cache'ini clearPlayer'a bağlama
4. `NextLevelAntiCheatPlatform` per-player state temizleme (`lastResultByPlayer`, `sessions`, `retention`)
5. `ActionRateLimiterService` clearPlayer desteği
