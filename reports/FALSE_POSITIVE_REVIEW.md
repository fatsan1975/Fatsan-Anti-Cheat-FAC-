# FALSE_POSITIVE_REVIEW

## Iteration 3 conclusions
- False positive azaltımı açısından en güçlü yeni adım, gürültülü ailelerin merkezi policy altında review-only / elevated-tier / disabled-default davranış almasıdır.
- Deep item context ve via-derived aileler için daha güvenli profile bazlı davranış çerçevesi oluştu.
- Feedback attribution artık son tetiklenen check ailesine bağlanabildiği için tuning verisi daha anlamlı hale geldi.

## Hâlâ dikkat isteyen aileler
- via/rewrite/window/skew/smear
- keepalive/traffic anomaly kümeleri
- deep item meta/lore/attribute/enchant kümeleri
- entropy/collapse/variance/plateau varyantları

## Production guidance
- default profile ile başla
- strict profile'a geçmeden önce feedback summary gözle
- custom mechanics olan sunucularda custom-mechanics-safe profile'ı değerlendir
