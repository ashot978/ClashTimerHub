### NL
- De All Timers-widget vraagt nu **iedere seconde** een nieuwe weergave van de timerregels aan zolang het scherm aanstaat en de bestaande live-widgetservice actief is, in plaats van iedere 15 seconden.
- De verversing loopt op secondegrenzen. Accountgegevens, widgetindeling, bestaande timers en de Sync-knop blijven ongewijzigd.
- Android en Samsung One UI bepalen uiteindelijk wanneer de launcher de widget tekent; onafgebroken zichtbare secondenupdates kunnen daardoor niet op alle toestellen worden gegarandeerd.

### EN
- The All Timers widget now requests refreshed countdown rows **every second** while the screen is on and the existing live-widget foreground service is running, instead of every 15 seconds.
- Refreshes are aligned to whole-second boundaries. Accounts, widget layout, stored timers and the Sync button are unchanged.
- Android and Samsung One UI ultimately control launcher rendering, so uninterrupted visible per-second updates cannot be guaranteed on every device.
