### NL
- Magic Snacks worden nu automatisch meegenomen bij een nieuwe Clash-export: Builder Bite via `boosts.builder_consumable` en Study Soup via `boosts.lab_consumable`.
- De All Timers-widget, het overzicht en de meldingen berekenen nu de verwachte eindtijd met Builder Bite (2× bouwsnelheid) en Study Soup (4× snelheid voor onderzoek en Pet House). De resterende snackduur wordt vanaf het exportmoment berekend.
- Overlap met drankjes wordt correct verwerkt: Builder Bite stapelt niet op een Builder Potion; Study Soup en Research Potion worden additief gecombineerd. Zo worden verlopen of overlappende boosts niet dubbel geteld.
- Nieuwe automatische tests controleren versnellingen, vervaltijden en overlapping. Importeer opnieuw terwijl de snack actief is; de app verzint geen actieve snack wanneer de export geen snackgegevens bevat.

### EN
- Active Magic Snacks are now imported from Clash exports: Builder Bite from `boosts.builder_consumable` and Study Soup from `boosts.lab_consumable`.
- All Timers, the app overview and notifications now calculate predicted finish times with Builder Bite (2× builder speed) and Study Soup (4× for research and Pet House), using the remaining snack duration at export time.
- Overlapping boosts are handled correctly: Builder Bite does not stack with Builder Potion; Study Soup combines additively with Research Potion. Expired and overlapping boosts are not double counted.
- New automated tests cover speeds, expiry and overlapping boosts. Sync again while the snack is active; a snack is never fabricated when the export omits its data.
