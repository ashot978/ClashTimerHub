# Clash Timer Hub V5 — Samsung Fold / Android

Een eigen Clash of Clans timerdashboard voor meerdere accounts, met één-tik-synchronisatie, actieve builders en een schaalbare homescreenwidget.

## Wat werkt in V5
- **Zwevende sync-knop:** kopieer in Clash of Clans via **Settings → Data Export → Copy** en tik één keer op het paarse zwaardje. Geen appwisseling of bevestiging. De knop wordt kort groen bij een succesvolle import of rood bij een fout.
- **Veilige accountkoppeling:** player tag heeft voorrang; anders wordt een bestaande accountnaam alleen gebruikt wanneer deze nog geen andere player tag heeft. Onbekende tags blijven afzonderlijk totdat je ze koppelt via **Accounts**.
- **Slimme importmelding:** aantal actieve, nieuwe, voltooide en gewijzigde upgrades.
- **Synchronisatie tussen schermen:** nieuwe data wordt opgeslagen in SharedPreferences, waarna de app de data herlaadt en de homescreenwidget direct wordt bijgewerkt.
- **V5 widget:** vergroot of verklein de widget; hij wisselt tussen compacte accountregels en uitgebreide kaarten met upgrade en eindtijd. Wanneer de launcher het ondersteunt, telt de chronometer continu af zonder periodieke app-polling. Tik op een account om de details in de app te openen.
- **Fold:** bij een breed scherm toont Dashboard de accounts links en de upgrades van het geselecteerde account rechts. Op het smalle scherm blijft de compacte dashboardweergave bestaan.
- **Statuskleuren:** groen voor klaar, goud voor bijna klaar en paars voor actief. Er zijn geen voortgangsbalken.

## Installeren
1. Ga naar [GitHub Actions](https://github.com/ashot978/ClashTimerHub/actions).
2. Open de nieuwste succesvolle **Build Clash Timer Hub V5**.
3. Download het artifact **ClashTimerHubV5-debug-apk**, pak het uit en installeer `app-debug.apk` over de bestaande app.
4. **Verwijder de bestaande app niet**: dan verdwijnen lokale accounts en timers.

Deze build behoudt de bestaande app-id en verwacht dezelfde Android-debug-ondertekeningssleutel. Als de signing-cache ontbreekt, stopt de build bewust in plaats van een APK te maken die niet over de huidige app kan worden geïnstalleerd.

## Widget toevoegen
Houd je startscherm ingedrukt → **Widgets** → **Clash Timer Hub**. Vergroot de widget om meer accounts te tonen. Een heel kleine widget kan niet alle zeven tegelijk tonen; hij geeft dan aan hoeveel accounts verborgen zijn.

## Timer- en alarmmeldingen
Er zijn twee verschillende tests onder Instellingen: **Testmelding versturen** controleert het notificatierecht; **Alarmtest over 1 minuut** controleert of een ingepland alarm afgaat. Zonder toestemming voor exacte alarmen kan Android echte meldingen later afleveren. Controleer op Samsung ook batterijbeperkingen en de instellingen voor exacte alarmen.

## Ontwikkelstatus
V5 is een beta. Elke codepush start een debug-APK-build op `main`. Controleer of de GitHub Actions-run groen is; sourcecode alleen bewijst niet dat de APK op elk toestel werkt.
