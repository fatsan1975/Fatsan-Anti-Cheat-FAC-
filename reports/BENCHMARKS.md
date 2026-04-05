# BENCHMARKS

## Iteration 3 benchmark-oriented findings
- `MovementEvent` hâlâ ana hot path (40 binding).
- `CombatHitEvent` ve `BlockBreakEventSignal` ikinci yoğun aileler (28'er binding).
- Tiered execution sonrası amaç, baseline oyuncularda her event ailesi için efektif evaluated-check sayısını düşürmektir.

## This iteration's structural gains
- Central action policy sayesinde via/deep-item/statistical aileler artık her zaman baseline'da çalıştırılmak zorunda değil.
- `FixedStepWindowTracker` ile üç benzer cadence/step kontrolünde ortak hesap mantığı kullanılmaya başlandı.
- Feedback attribution sayesinde tuning odaklı benchmark/FP analizi yapılabilir hale geldi.

## Next benchmark target set
1. Baseline vs Elevated vs Hot evaluated-check count comparison.
2. `MovementEvent` policy-gated evaluation count.
3. `FixedStepWindowTracker` kullanan ve kullanmayan varyantların micro-cost farkı.
4. Telemetry sampling öncesi/sonrası maliyet kıyası.
