### NL
- Nieuwe optionele instelling **Aftellers altijd actief houden** in de All Timers-widgetinstellingen. Deze staat standaard aan en kan per widget worden uitgezet.
- Een aparte voorgrondservice houdt de timerregels actief zolang minstens één widget deze stand gebruikt. Bij een ingeschakeld scherm vraagt de app ongeveer iedere 15 seconden een nieuwe weergave van de countdowns aan. Bij het aanzetten van het scherm volgt direct een vernieuwing.
- Android toont hiervoor een blijvende Clash Timer Hub-melding. Wanneer geen widget de instelling gebruikt, stopt de service.
- De app probeert de live service na herstart, app-update en het openen van Clash Timer Hub te herstellen. Android of Samsung kan updates nog steeds beperken; ononderbroken secondenweergave is niet gegarandeerd. Deze stand kost extra batterij.

### EN
- New optional **Keep countdowns always active** setting in the All Timers widget configuration. Enabled by default and configurable per widget.
- A dedicated foreground service keeps account timer rows refreshed while at least one widget uses the setting. With the screen on, it requests new countdown views about every 15 seconds and refreshes immediately when the screen wakes.
- Android displays an ongoing Clash Timer Hub notification while the service is active, and the service stops when no widgets require it.
- The app attempts to restore live updates after restart, app updates, or reopening Clash Timer Hub. Android and Samsung can still throttle rendering; uninterrupted per-second animation cannot be guaranteed. This mode uses extra battery.
