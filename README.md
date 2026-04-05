# FAC_Folia

Fatsan Anti Cheat (FAC), Minecraft Java 1.21.11 için Folia-first anti-cheat pluginidir.

## Bu iterasyonun öne çıkanları
- Context-aware check routing (event türüne göre çalıştırma)
- Risk fusion (weighted + decay)
- Folia-safe action pipeline (`ALERT`, `SETBACK`, `KICK`)
- Player safe-location tracking ile setback
- Multi-domain check set (movement/combat/world/inventory/protocol)
- Premium lisans doğrulama + feature gating
- Premium webhook alert dispatch
- `/fac premium` ile premium operasyonel özet

## Premium roadmap
Premium seviyeye geçiş planı ve repo içi implementasyon özeti:
- `docs/PREMIUM_ROADMAP.md`

## Next-level roadmap implementation
Faz A-E (50 başlık) için kod karşılıkları:
- `docs/NEXT_LEVEL_IMPLEMENTATION.md`

## Build/Test
```bash
./gradlew --no-daemon test --console=plain
./gradlew --no-daemon build -x test --console=plain
```
