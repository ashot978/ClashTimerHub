### NL
- Hersteld: boostberekeningen voor bouwers, onderzoek en pets gebruiken nu consequent het **tijdstip van de Clash-export**. Voorheen begon de voorspelling pas bij het importeren, terwijl de exporttimers al eerder waren gemeten.
- Dit kon vooral bij een actieve Builder Potion (10×) een merkbare afwijking geven: 90 seconden tussen exporteren en synchroniseren kon de voorspelling circa 13 minuten en 30 seconden te laat zetten.
- Builder Bite (2×) wordt niet bovenop een actieve Builder Potion (10×) vermenigvuldigd. De oorspronkelijke eindtijden en bestaande upgrades blijven bewaard.
- Nieuwe regressietests controleren een vertraagde sync en gelijktijdige Builder Potion en Builder Bite. Synchroniseer Ashotgun opnieuw om de voorspelling op het juiste exporttijdstip te baseren.

### EN
- Fixed: boosted builder, research and pet predictions consistently use the **original Clash export timestamp**. Previously, boost forecasts started at clipboard import time while export timers were measured earlier.
- With an active 10× Builder Potion, a 90-second export-to-import delay could make a predicted completion about 13 minutes and 30 seconds too late.
- A 2× Builder Bite never multiplies on top of an active 10× Builder Potion. Saved source timers and upgrades are preserved.
- New regression tests cover delayed sync and overlapping Builder Potion and Builder Bite. Sync the affected account again to establish the correct export-time baseline.
