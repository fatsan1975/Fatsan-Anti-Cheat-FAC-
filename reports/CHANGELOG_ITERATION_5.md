# CHANGELOG_ITERATION_5

## Tarih
2026-03-19

## Özet
Iteration 5 dört teknik eksen üzerinde yoğunlaşmıştır:
1. `AbstractWindowCheck` base class ile check mimarisi temizlendi
2. 6 check daha `WindowStatsTracker`'a taşındı (toplam 8 check artık WindowStats kullanıyor)
3. Player quit memory leak tam olarak kapatıldı — `Check.onPlayerQuit` → `CheckRegistry.clearPlayer` → `BukkitSignalBridge.onQuit` zinciri kuruldu
4. `ActionPolicyService`'e `minigame`, `survival`, `via-heavy` profil desteği eklendi

---

## 1. AbstractWindowCheck — Yeni base class

**Dosya:** `check/AbstractWindowCheck.java`

`AbstractBufferedCheck` + `WindowStatsTracker`'ı birleştiren ortak base class.

**Sağlanan özellikler:**
- `protected final WindowStatsTracker stats` field'ı (window boyutu constructor'da belirtilir)
- `onPlayerQuit(playerId)` override — hem `AbstractBufferedCheck` buffer'ını hem de tracker'ı temizler
- Tek `super(bufferLimit, windowSize)` constructor çağrısı yeterli

**WindowStatsTracker kullanan check'ler artık `AbstractWindowCheck` extend eder:**

| Check | Window Boyutu |
|---|---|
| `ReachVarianceCollapseCheck` | 8 |
| `CombatIntervalEntropyCheck` | 8 |
| `InventoryIntervalEntropyCheck` | 8 |
| `BreakIntervalVarianceCollapseCheck` | 8 |
| `KeepAliveJitterCollapseCheck` | 10 |
| `CombatHitIntervalPlateauCheck` | 8 |
| `MovementGroundSpeedPlateauCheck` | 8 |
| `RotationYawVarianceCollapseCheck` | 8 |

**Test:** `check/AbstractWindowCheckTest.java`

---

## 2. Check.onPlayerQuit — Interface genişletmesi

**Dosya:** `check/Check.java`

```java
default void onPlayerQuit(String playerId) {}
```

Tüm check'ler bu default'u miras alır. `AbstractBufferedCheck` buffer'ı temizleyen bir
override ekler. `AbstractWindowCheck` hem buffer hem tracker'ı temizler.

---

## 3. AbstractBufferedCheck.onPlayerQuit

**Dosya:** `check/AbstractBufferedCheck.java`

`buffers.remove(playerId)` ile buffer map'inden player state'ini kaldırır.
Artık oyuncu ayrıldığında buffer state hafızada kalmıyor.

---

## 4. CheckRegistry.clearPlayer

**Dosya:** `engine/CheckRegistry.java`

```java
public void clearPlayer(String playerId)
```

Tüm kayıtlı check'leri iterate eder ve her birinin `onPlayerQuit(playerId)`'ini çağırır.
Tek bir `BukkitSignalBridge.onQuit` çağrısıyla tüm check state'i temizlenir.
Exception handling: cleanup hatası disconnect event'ini asla bloklamamalı.

---

## 5. BukkitSignalBridge.onQuit — clearPlayer wire-up

**Dosya:** `packet/BukkitSignalBridge.java`

```java
intake.registry().clearPlayer(playerId);
```

Player quit'te:
1. `tracker.clear(uuid)` — signal interval tracker
2. `playerStateService.clear(uuid)` — safe location
3. `intake.registry().clearPlayer(playerId)` — tüm check state

**Memory leak durumu:** Artık yüksek turnover'lı sunucularda (BungeeCord, hub'lar)
ayrılan oyuncuların `ConcurrentHashMap` entry'leri temizleniyor.

---

## 6. ActionPolicyService — Minigame, Survival, Via-Heavy profil desteği

**Dosya:** `service/ActionPolicyService.java`

### Eklenen profiller

#### `minigame`
- **Teleport-heavy famileler** (`TeleportChain`, `TeleportBurstFollow`, `RegionIoFusion` vb.):
  `REVIEW_ONLY` + `ELEVATED` tier — sık teleport gerektiren ortamlar için güvenli
- **Build/World famileleri** (`ScaffoldPattern`, `FastBreak`, `BlockBreak*`, `BlockPlace*`):
  `ALERT_ONLY` + `ELEVATED` tier — minigame'de scaffold/fastbreak normal davranış olabilir
- **Statistical famileler**: `HOT` tier — gürültülü ortamda konservatif

#### `survival`
- Belgelenmiş ama davranış `default`'tan farklılaşmıyor (gelecek iteration için ayrılmış)
- `isSurvival()` predicate eklendi

#### `via-heavy`
- **Via/timing-derived famileler**: `REVIEW_ONLY` + `HOT` tier (strict profile'dan da daha konservatif)
- Via/rewrite/window/smear/skew ailesi hiçbir zaman punish trigger'ı olmuyor

### Mevcut profillerin iyileştirilmesi

#### `strict`
- Via-derived famileler artık `ELEVATED` tier (önceden `HOT` idi) — daha hızlı değerlendirme
- Statistical famileler `ELEVATED` tier (önceden `HOT`) — strict ortamlarda daha erken algılama

**Test:** `ActionPolicyServiceProfileTest.java` (25 senaryo)

---

## 7. RotationYawVarianceCollapseCheck — Kod kalitesi iyileştirmesi

**Dosya:** `check/RotationYawVarianceCollapseCheck.java`

Minified tek-satır kod → `AbstractWindowCheck` extend eden, tam dokümantasyonlu sınıf.
Detection reason mesajı artık mean ve CV değerlerini içeriyor.

---

## Performans etkisi

- **clearPlayer**: `O(N)` iterasyon (N = kayıtlı check sayısı, yaklaşık 182), ancak sadece
  player disconnect'te çalışır — hot path'te hiçbir etkisi yok.
- **AbstractWindowCheck.onPlayerQuit**: `ConcurrentHashMap.remove()` ×2 (buffer + tracker),
  negligible.
- **ActionPolicyService**: Yeni profil metodları (`isMinigame()`, `isViaHeavy()` vs.)
  string equality check — completely free.

## False positive etkisi

- **minigame profili**: Scaffold/fastbreak/teleport check'leri artık minigame'de yanlış ban
  tetikleyemiyor — bu check'ler sadece alert üretiyor.
- **via-heavy profili**: Via-derived aileler hiçbir zaman setback/kick'e gidemiyor,
  sadece review queue'ya düşüyor.
- **clearPlayer**: State temizlenmeden kalan eski oyuncu verileri artık yeni oyuncunun
  (aynı slot/UUID) false positive almalarına yol açmıyor.

## Test coverage

| Dosya | Senaryo sayısı |
|---|---|
| `AbstractWindowCheckTest.java` | 4 |
| `ActionPolicyServiceProfileTest.java` | 25 |

**Toplam test sayısı:** 82 (önceki: 78)

## Sonraki adımlar (Iteration 6 önerileri)

1. `EvidenceService` temizleme — player quit'te evidence map'ten de player silme
2. `RiskService`, `PlayerTrustService`, `SuspicionPatternService`, `CorroborationService`
   temizleme — tüm per-player state'lerin quit'te silinmesi
3. `survival` profil davranışını farklılaştırma — block-break/world ailesi için hafif
   daha agresif threshold
4. Yeni check ailesi: `MovementInertiaBreakCheck` ve `AirStrafeAccelerationCheck` için
   de WindowStatsTracker migration
5. Benchmark raporu: tiered execution skip-ratio baseline vs elevated karşılaştırması
