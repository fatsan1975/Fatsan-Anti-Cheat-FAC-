# CHANGELOG — ITERATION 7

**Tarih:** 2026-03-19
**Durum:** BUILD SUCCESSFUL — 188 test, 0 hata

---

## Genel Bakış

Iteration 7; build kırıklığını düzeltti, Iteration 6'da açık bırakılan tüm eksikleri kapattı ve
CombatHitDistancePlateauCheck / AirStrafeAccelerationCheck / MovementInertiaBreakCheck check'lerini
AbstractWindowCheck mimarisine taşıdı.

---

## Düzeltmeler

### Build kırıklığı — WindowStatsTracker.Stats import eksikliği
8 check `var` ile düzeltildi. `io.fatsan.fac.check` paketindeki sınıflar `io.fatsan.fac.check.support.WindowStatsTracker`'ı explicit import etmeden `WindowStatsTracker.Stats` tipini kullanıyordu. Java 21 `var` type inference ile temiz çözüm uygulandı.

**Etkilenen dosyalar:**
- `BreakIntervalVarianceCollapseCheck`
- `CombatHitIntervalPlateauCheck`
- `CombatIntervalEntropyCheck`
- `InventoryIntervalEntropyCheck`
- `KeepAliveJitterCollapseCheck`
- `MovementGroundSpeedPlateauCheck`
- `ReachVarianceCollapseCheck`
- `RotationYawVarianceCollapseCheck`

### SeverityNormalizer — 2 test hatası
- `InventoryPacketBundleDesync` (0.55 bekleniyor, 0.70 dönüyordu): `isTimingDerived` "bundle" keyword'ü eklendi.
- `KeepAliveDrift` (0.70 bekleniyor, 0.65 dönüyordu): `isProtocolNoiseFamily` kontrolü `isStatisticalFamily`'den önce yapılacak şekilde sıra değiştirildi.

---

## Yeni Özellikler

### 1. ProtocolProfileResolver.clearPlayer
```
clearPlayer zinciri (tam):
BukkitSignalBridge.onQuit
  → tracker.clear()
  → playerStateService.clear()
  → registry.clearPlayer()          # 182 check
  → engine.clearPlayer()
      → riskService.clearPlayer()
      → playerTrustService.clearPlayer()
      → suspicionPatternService.clearPlayer()
      → corroborationService.clearPlayer()
      → evidenceService.clearPlayer()
      → actionService.clearPlayer()
          → actionRateLimiterService.clearPlayer()
          → protocolProfileResolver.clearPlayer()  ← YENİ
      → nextLevelPlatform.clearPlayer()
```

### 2. ActionPolicyService — survival profil farklılaştırması
Survival sunucuda build/world statistical aileler daha erken değerlendiriliyor:

| Aile | default | survival |
|---|---|---|
| BreakIntervalVarianceCollapse | HOT | ELEVATED |
| BlockBreakCadence | HOT | ELEVATED |
| BlockPlaceCadenceEntropy | HOT | ELEVATED |
| CombatIntervalEntropy | HOT | HOT (değişmedi) |

**Mantık:** Survival'da nuker/speedmine daha kritik hasar verir; bu check'lerin ELEVATED tier'da aktive edilmesi daha erken uyarı sağlar.

### 3. CombatHitDistancePlateauCheck — tam rewrite
- **Eski:** Minified tek-satır, `streak` + `last` map pair, bypass vektörü: tek outlier frame streak'i sıfırlıyor.
- **Yeni:** `AbstractWindowCheck` (window 8), `isUniformlyCadenced(0.015)`, tüm window'un sabit olması gerekiyor.
- **Threshold değişimi:** `reachDistance > 2.9 && |prev - cur| < 0.01` → `ws.mean() > 2.9 && cv < 0.015`.

### 4. AirStrafeAccelerationCheck — AbstractWindowCheck migration
- **Memory leak kapatıldı:** `lastAirSpeedBps` map player quit'te temizlenmiyordu.
- **Yeni detection:** `peek()` ile window mean (established baseline) → `speedBps > 12.0 && (speedBps - prev.mean()) > 6.0`.
- **Landing reset:** `stats.clear()` her ground touch'ta — airborne phase'ler arası baseline karışmıyor.

### 5. MovementInertiaBreakCheck — AbstractWindowCheck migration
- **Memory leak kapatıldı:** `lastDelta` map player quit'te temizlenmiyordu.
- **Yeni detection:** `peek()` ile window mean → `prev.mean() > 0.18 && deltaXZ < 0.02 && (deltaXZ - prev.mean()) < -0.16`.
- Eşik değerleri korundu, sadece "son değer" yerine "window mean" referans alınıyor.

### 6. EvidenceService snapshot → /fac dq komutu

```
/fac dq <playerId>    → son 10 evidence record (checkName, severity, reason)
/fac dq               → global telemetri (eski davranış korundu)
```

`AntiCheatEngine.evidenceSnapshot(String playerId)` getter eklendi.

---

## Test Özeti

| Dosya | Durum |
|---|---|
| `AirStrafeAccelerationCheckTest` | Güncellendi — 4 senaryo |
| `MovementInertiaBreakCheckTest` | Yeni — 4 senaryo |
| `CombatHitDistancePlateauCheckTest` | Yeni — 3 senaryo |
| `ActionPolicyServiceProfileTest` | +4 survival senaryosu |
| `SeverityNormalizerTest` | Tümü geçiyor (2 düzeltme sonrası) |
| **Toplam** | **188 test, 0 hata, BUILD SUCCESSFUL** |
