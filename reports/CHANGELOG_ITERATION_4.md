# CHANGELOG_ITERATION_4

## Summary
Iteration 4 tamamlandı. Bu iterasyon üç teknik eksen üzerinde yoğunlaşmıştır:
1. Shared extractor kapsamının genişletilmesi (variance/entropy ailesi)
2. Family-level severity normalisation ile risk skorunun daha gerçekçi hale getirilmesi
3. Operator görünürlüğünün artırılması (eval-count telemetrisi ve `/fac status` genişletmesi)

## 1. WindowStatsTracker — yeni shared extractor

**Dosya:** `check/support/WindowStatsTracker.java`

- Sliding window istatistik hesabı (mean, variance, stddev, oscillation count,
  entropy score) tek bir thread-safe utility'de toplandı.
- Daha önce `ReachVarianceCollapseCheck`, `CombatIntervalEntropyCheck` ve
  diğer entropy/variance check'leri her biri kendi `ConcurrentHashMap<String, Deque<X>>`
  ve manuel variance hesabı tutuyordu.
- `WindowStatsTracker.Stats` record'u check'lere tutarlı bir API sağlar:
  `isFlat(maxVariance)`, `isOscillating(minOscillations)`, `isUniformlyCadenced(maxCv)`.
- Test: `check/support/WindowStatsTrackerTest.java` (12 senaryo).

## 2. SeverityNormalizer — family-level severity cap

**Dosya:** `service/SeverityNormalizer.java`

Sorun: Bazı check aileleri (deep-item-context, via-derived, statistical) yüksek
raw severity üretip risk skoru üzerinde gerçek signal kalitesiyle orantısız etki
yaratabiliyordu.

Çözüm: Her check için ailesine göre bir cap uygulandı:

| Aile | Cap |
|---|---|
| deep-item-context (attribute/enchant/meta/lore) | 0.45 |
| via-derived / timing-derived | 0.55 |
| statistical (entropy/variance/collapse/plateau) | 0.65 |
| protocol noise (keepalive/ping/traffic) | 0.70 |
| core punish candidates (speed/reach/nofall/scaffold/fastbreak) | 1.0 |
| movement fallback | 0.90 |
| combat fallback | 0.85 |
| world fallback | 0.80 |
| inventory fallback | 0.70 |

- `RiskService.apply(...)` artık `SeverityNormalizer.normalize(...)` çağrısı yapar.
- Test: `SeverityNormalizerTest.java` (25 senaryo).

## 3. ReachVarianceCollapseCheck — WindowStatsTracker'a taşındı

- Duplicate `ConcurrentHashMap<String, Deque<Double>>` + manuel variance hesabı kaldırıldı.
- `WindowStatsTracker` ile aynı detection mantığı daha az kod ve daha fazla açıklama ile sağlandı.
- Detection reason mesajı artık mean ve CV değerlerini içeriyor (operator triage için).

## 4. CombatIntervalEntropyCheck — WindowStatsTracker'a taşındı

- Duplicate `ConcurrentHashMap<String, Deque<Long>>` + `distinct().count()` tabanlı
  basit unique-değer kontrolü kaldırıldı.
- `WindowStatsTracker.Stats.isUniformlyCadenced(maxCv)` ile daha hassas entropi kontrolü yapılıyor.
- Detection reason mesajı artık mean (ms) ve CV değerlerini içeriyor.

## 5. CheckRegistry — eval-count telemetrisi

**Yeni alanlar:** `totalEvaluations` (LongAdder), `totalSkipped` (LongAdder)

**Yeni metodlar:**
- `totalEvaluations()` — startup'tan bu yana çalıştırılan toplam check sayısı
- `totalSkipped()` — tier/policy gating tarafından atlanan toplam check sayısı
- `registeredCheckCount()` — registry'deki toplam aktif check sayısı

`evaluateAll(...)` içinde her skip → `totalSkipped.increment()`, her evaluate → `totalEvaluations.increment()`.

## 6. AntiCheatEngine — eval stats proxy'leri

**Yeni metodlar:** `totalEvaluations()`, `totalSkipped()`, `registeredCheckCount()`  
Registry'nin counter'larını üst katmana expose eder.

## 7. /fac status — genişletilmiş çıktı

Önceki çıktı:
```
[FAC] status profile=... policy=... action=... processed=X suspicious=Y
```

Yeni çıktı (iki satır):
```
[FAC] status profile=... policy=... action=... processed=X suspicious=Y
[FAC] checks=182 evaluations=14523 skipped=4211 skip-ratio=22.5%
```

`skip-ratio`, tiered execution'ın gerçekte ne kadar check'i eleyip ellemediğini
operatöre doğrudan gösterir.

## 8. config.yml — tam ve yorumlanmış default

`src/main/resources/config.yml` artık boş değil.  Her alan Türkçe yorum satırları
olmaksızın (Spigot YAML formatter uyumluluğu için) okunabilir inline comment'lerle
İngilizce dokümante edilmiştir.

## Performans etkisi

- `SeverityNormalizer` — tek String normalize + switch expression; her event'te
  maliyetsiz.
- `WindowStatsTracker` — synchronized deque erişimi, O(n) istatistik hesabı;
  window boyutu ≤ 8 olduğu için pratikte sabit maliyet kabul edilebilir.
- `totalEvaluations` / `totalSkipped` — `LongAdder` increment; contention-free.

## False positive etkisi

- `SeverityNormalizer` cap'leri: deep-item ve via-derived aileler artık risk skoru
  üzerinde daha orantılı etki yaratıyor; yanlış ban olasılığı bu aileler için azalıyor.
- `ReachVarianceCollapseCheck` için `isUniformlyCadenced(0.025)` önceki
  `variance < 0.003` threshold'una kıyasla daha stabil (outlier değerlere daha az duyarlı).
- `CombatIntervalEntropyCheck` için `distinct().count() <= 2` yerine CV tabanlı
  kontrol daha az kenar durumda yanlış tetiklenir.

## Sonraki adımlar

1. `InventoryIntervalEntropyCheck`, `BreakIntervalVarianceCollapseCheck` ve
   `KeepAliveJitterCollapseCheck` için de WindowStatsTracker migration.
2. Tiered execution için `skip-ratio` karşılaştırmalı benchmark raporu.
3. Family-level severity normalization → feedback summary → tuning döngüsü belgesi.
4. `ReachVarianceCollapseCheckTest` genişletme (WindowStatsTracker entegrasyon testi).
