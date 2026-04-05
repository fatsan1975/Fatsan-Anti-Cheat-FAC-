# CHANGELOG_ITERATION_3

## Central action policy
- `ActionPolicyService`, `ActionDisposition`, `SuspicionTier` eklendi.
- Action safety kararları merkezileştirildi.
- `ActionService` policy-aware hale getirildi.

## Tiered execution
- `AntiCheatEngine` risk/trust/recent-intensity üzerinden suspicion tier hesaplıyor.
- `CheckRegistry` artık policy+tier ile evaluation gate uygular.

## Shared extractor
- `FixedStepWindowTracker` eklendi.
- Combat/block-break/inventory cadence-step ailesinin bir kısmı ortak extractor'a taşındı.

## Feedback/tuning
- false-positive feedback artık son tetiklenen check ile ilişkilendirilebiliyor.

## Docs/reports
- action safety, performance, compatibility, tuning, tiered execution, shared extractor ve tuning loop belgeleri güncellendi/eklendi.
