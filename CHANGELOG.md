# ClashTimerHub release notes

## All Timers: compact widgetvlak en bereikbaar tandwiel/opslaan — 6 november 2026

### NL
- Vervang de pijltjesknop rechtsboven door een tastbaar 48dp-breed ⚙-tandwiel in de footer rechtsonder, direct naast de Sync ON/OFF-bediening.
- Maak het zichtbare widgetpaneel net zo hoog als de accountlijst in plaats van een paarse rechthoek over alle gereserveerde launcherhoogte. Extra launcherhoogte blijft transparant. De launcher kan de gereserveerde ruimte alleen zelf aanpassen via lang indrukken/verslepen.
- Op Android 12+ past de lengte van de lijst zich aan het aantal geselecteerde accounts en de widgethoogte aan. Wordt de widget smaller, dan worden de timers in twee of drie nette regels per account verdeeld en blijft de lijst scrollbaar.
- De widget ondersteunt nu ook verkleinen tot circa 130dp. De footer met Sync en instellingen blijft zichtbaar, ook bij minder hoogte.
- De Potion Controls-sectie en beide beheerknoppen zijn verwijderd uit het configuratiescherm; Boosts worden nog steeds via bestaande accounttimer-acties en app-instellingen beheerd.
- Voorkom dat Android 15+/Samsung taakbalk en navigatiebalk de Save Widget-knop afdekken door expliciete statusbalk- en navigatie-insets te gebruiken. De opslaanknop blijft vast onder het scrollbare instellingendeel en boven de systeembalk.
- Laat bestaande sorteervolgorde, kolomconfiguratie, potionberekeningen, notificaties, accounts en app-signing ongemoeid.

### EN
- Bottom-right 48dp settings gear replaces the top-right arrow; footer Sync remains accessible.
- Wrap purple widget panel to the content and dynamically size the account list using widget size and row count. Keep remaining launcher allocation transparent. Reflow to two/three lines for narrow widgets.
- Remove obsolete Potion Controls from All Timers configuration. Leave Potion editing elsewhere intact.
- Handle Android 15+ status/navigation/taskbar insets so sticky Save Widget is always visible and tappable.
- Preserve sorted accounts, potion-adjusted predicted completion, notifications and existing user data.

## Eén notificatie-instelling; zwaarden op Sync, wizard alleen in resultaat — 6 november 2026

### NL
- Meldingsinstellingen zijn niet langer dubbel: alleen aan/uit, melding bij afronding en X minuten vooraf. De tijd is gebaseerd op dezelfde voorspelde eindtijd (inclusief actieve potions) als All Timers.
- De aparte Potion-notificatieopties en hun uitvoer naar nieuwe back-ups zijn verwijderd. Bestaande data en oude back-ups blijven bruikbaar.
- Herstel het originele ⚔-icoon op de zwevende Sync-knop, de 1×1 Sync-widget en het menu Synchronisatie. Ook na de tijdelijke ✓/!-flash herstelt de knop naar ⚔.
- Toon 🧙 uitsluitend in de tijdelijke synchronisatie-resultaatmelding, zowel bij succes als mislukking.
- De nieuwe uniforme alarmplanner onderdrukt/cancelt oude Potion-alarmen en bouwt één melding per upgrade per gebeurtenissoort op de ingekorte eindtijd.

### EN
- One common notification settings dialog for normal and Potion-affected upgrades: enable, finish and early minutes.
- Keep crossed swords on floating and 1x1 Sync buttons and app menu, including after the feedback flash. Show wizard in temporary Sync-result popup only.
- Old Potion notification settings are no longer shown or saved in new backups; legacy backups and imported timers remain compatible.

## Behoud interne timerverversing bij widget-opruiming — 6 november 2026

- De oude multi-account-widget wordt niet meer in de widgetkiezer getoond, maar de bijbehorende TimerWidget BroadcastReceiver blijft intern actief voor reeds geplande timerverversingen en alarmrescheduling.
- Android registreert deze receiver zonder APPWIDGET_UPDATE intent-filter of appwidget-provider metadata; hierdoor ontstaat geen extra widgetkeuze.
- De 4 zichtbare widgets, Potion-controls en Wizard-Sync veranderen niet.

## Vier widgets, wizard Sync en snelle Potion-bediening — 6 november 2026

### NL
- Beperk de Android widgetkiezer tot vier opties: All Timers (alle accounts), mini-account 1×1, losse accountwidget, 1×1 Sync-knop. Verwijder de aparte Builder Potion-/Research Potion-overzichten en de oude multi-accountwidget uit de widgetkiezer.
- Herstel de 🧙-icoon van de zwevende Sync-knop (ook nadat ✓/! feedback verdwijnt), synchroniseer het icoon in de 1×1 Sync-widget en op de synchronisatiepagina in de app.
- Bewaar snelle Potion-bediening zonder aparte widgets: tik in All Timers op het Builder- of Research-timerblok van een account om rechtstreeks het exacte per-account Potion-scherm te openen. Tik op Pets of de accountnaam voor de accountdetails.
- Onder All Timers-configuratie en in de app-instellingen blijven aparte knoppen naar Builder en Research Potion-beheer: +1 uur, exacte resterende duur, wissen.
- Bestaande potiontrackers, oude en versneld berekende upgrades, MAX, meldingen, app-ID, signing en opgeslagen voorkeuren zijn niet gewijzigd.
- Reeds geplaatste widgets van verwijderde provider-types kunnen door Android uit het startscherm verwijderd worden. Opnieuw toevoegen als All Timers. Originele gebruikerdata wordt niet gewist.

### EN
- Retain only four launcher widget choices: All Timers, 1×1 mini-account, single-account widget and 1×1 Sync.
- Restore wizard icon on floating Sync and its post-feedback state; use matching wizard on the 1×1 Sync shortcut and app settings.
- Preserve fast per-account +1h/exact-duration potion controls by tapping Research/Builders category groups on All Timers, with backup access from widget configuration and app Settings.
- Do not remove any underlying potion tracking, alarm scheduling, accounts or signed app identity. Old legacy widgets disappear from the Android widget picker (and installed instances may need replacing).

## Instelbare All Timers-widget met boosted buildersortering — 6 november 2026

### NL
- De standaardkolomvolgorde is Research, Pets, Builders; elke categorie houdt het eigen aftelklokje en de eigen eindkloktijd strak bij elkaar.
- De widget sorteert accounts standaard op de eerstvolgende bouwer die klaar is, inclusief de voorspelde invloed van Builder Potions. Gereed-voor-sync bouwers staan bovenaan.
- Per widget afzonderlijk in te stellen: kolomvolgorde met ↑/↓, sortering op volgende bouwer/alfabetisch/eigen volgorde, alle of geselecteerde accounts, eigen accountvolgorde en helperweergave.
- Open configuratie via de pijl in de widget, bij toevoegen via Android of vanuit Instellingen → Widget instellen in de app.
- Groen betekent een al potion-gecorrigeerde echte resterende duur en bijbehorende ingekorte eindkloktijd. Gewone waarden blijven goud en lavendel, MAX-vinkjes blijven groen.
- De bestaande Potion-alarmen rekenen met dezelfde onderliggende Builder/Research Potion-voorspelling en worden bij sync opnieuw ingesteld. De aangepaste widgetsortering verandert de alarmtijden niet.
- Behoud alle geïnstalleerde widget-ID's, originele JSON, andere widgets, accountinstellingen, app-ID en ondertekening.

### EN
- Default All Timers category order is Research, Pets, Builders, keeping each countdown tightly paired with its actual finish clock.
- Sort by earliest Potion-adjusted builder completion, with completed-but-awaiting-sync first. Alternative per-widget sort modes: alphabetical or manual.
- Per-widget configuration includes draggable via arrow category order, all/selected accounts, manual account ordering and helpers visibility.
- Configuration opens from the widget arrow, Android add-widget flow and the app settings.
- Keep predicted realtime boost-aware countdowns green and normal values gold/lavender; preserve checked MAX indicators, existing alarms and app identity.

## All Timers: categorieparen op één plek — 6 november 2026

### NL
- Plaats binnen elk account steeds de bijbehorende aftelling en eindkloktijd naast elkaar: bouwersicoon + timer + eindklok, daarna onderzoeksicoon + timer + eindklok, daarna petsicoon + timer + eindklok.
- Herstel dit in de brede, compacte en smalle layouts. Smal plaatst pets op een derde regel, maar wisselt de kolommen niet door elkaar.
- Groen betekent dat het getoonde eindmoment al potion-versneld is. De countdown telt de werkelijk resterende uren/minuten/seconden af tot die berekende eindklok; hij loopt dus niet nog eens 10× of 24× versneld.
- Geen nieuwe bliksem-symbolen, geen timeline; originele iconen, MAX-vinkjes en bestaande Sync ON/OFF blijven behouden. Bestaande prognoseformules en notificaties worden niet veranderd.

### EN
- Show builder icon/countdown/finish as one adjacent pair, followed by research and pet pairs, across all widget widths.
- For small widths, the pet pair moves to a third line rather than separating the timers from finish times.
- Boosted times are already real-world duration until the 10x/24x accelerated predicted finish; the Chronometer counts down at normal speed.
- Preserve original category icons, MAX states, original boost calculations and Sync toggle.

## Timerblokken links, kloktijden rechts — geen Timeline of bliksem — 6 november 2026

### NL
- De All Timers-widget bevat voortaan uitsluitend het multi-accountoverzicht. De extra 'ALL UPGRADES'-sectie, scrollbare upgrade-timeline en individuele upgrade-rijen zijn uit deze widget verwijderd.
- Elk account toont nu op vaste volgorde bouwers-, onderzoeks- en pet-aftellingen als groep links; daarna de drie bijbehorende eindkloktijden als groep rechts. Dit is niet meer aftelling-eindtijd-afwisseling per categorie.
- De categorie-iconen ⚒, 🧙 en 🐾 blijven altijd zichtbaar en veranderen nooit in een bliksemsymbool. De widget bevat geen bliksem of bliksemlegenda meer; alleen de groene tekst onderscheidt Potion-verkorte aftellingen/eindtijden.
- MAX-✓ en het onderscheid met — of klaar-voor-sync ! blijven behouden. Normale aftellingen goud, normale eindklokken lavendel, versnelde waarden groen.
- Ashotgun 6:44 resterend en 'morgen 00:00' was op het getoonde toestel rond 17:16 mathematisch consistent, maar de feitelijke correcte eindtijd vereist nieuwe game-export om te valideren. Daarom zijn de Potion-algoritmes niet willekeurig aangepast.
- De widget wordt lager van standaardformaat (4×3), met reeds geplaatste widgets, accounts, overige widgets en notificaties behouden.

### EN
- All Timers now shows only account-level countdowns, with no timeline or ALL UPGRADES rows.
- Render all three countdown columns together on the left, followed by their three clock-time columns together on the right. Uniform order in wide/compact/narrow layouts.
- Preserve the original builders/research/pets icons; never substitute a lightning emoji. Green text alone indicates Potion-adjusted forecasts.
- Keep checked MAX marks and existing alert logic. Reduce default widget height to 4×3.
- Ashotgun's displayed countdown and midnight finish agree arithmetically, but actual correctness still requires fresh game-export comparison.

## MAX-vinkjes terug en uniforme kleuren/tijdvolgorde — 6 november 2026

### NL
- De groene ✓ verschijnt alleen voor expliciet handmatig als MAX aangevinkte bouwers-, laboratorium- of petcategorieën, op basis van AppData.isMaxed, zoals in de oorspronkelijke multi-accountwidget.
- Geen actieve timer zonder MAX geeft —; een door boost voorspelde voltooiing die nog moet worden gesynchroniseerd geeft ! / Klaar en is dus niet hetzelfde als MAX.
- Uniforme kleuren: normale aftelduur goud, normale eindkloktijd en categorie-icoon lavendel, boosted aftelling én eindkloktijd groen, MAX-vinkje groen.
- Volgorde altijd eerst aftelling, daarna eindkloktijd; ook in de lijst met alle individuele upgrades (aftelling op regel 1, eindklok op regel 2 na de boostindicator).
- Geen ongeteste Pet Potion-gegevens ingelezen. Originele timers en meldingen behouden.

### EN
- Restore user-checked MAX categories as green ticks, distinct from idle and boosted-ready, reusing AppData.isMaxed for builders, lab and pets.
- Unified gold countdown, muted lavender end clock/icon, green boosted countdown+finish and green MAX state.
- Put remaining duration before actual finish time everywhere, including individual upgrade rows.
- Do not guess Pet Potion values; keep original alarms and accounts intact.

## Compacte All Timers-widget zonder lege regels — 6 november 2026

### NL
- Verwijder het onnodig hoge drieregels-overzicht op Samsung Fold: wanneer er minstens 325dp ruimte is gebruiken alle accounts nu precies 2 regels (naam + bouwers/helpers boven, drie timerparen met resterende tijd én eindkloktijd direct naast elkaar eronder).
- De zeven accountregels gaan van 69dp naar 44dp: ongeveer 36% minder hoogte. De terugkerende '⚡ = boost'-legenda per account is verwijderd; bovenaan blijft de algemene legenda staan.
- Vanaf 665dp gebruikt de widget één horizontale regel per account, met kleinere en strakkere kolommen. Alleen onder 325dp blijft de oorspronkelijke drieregels-indeling als veilige fallback.
- Eindkloktijden blijven zichtbaar dankzij een kortere datumweergave zoals '9/11 23:55' in de compacte indeling.
- Compactere standaardhoogte (4×4 in plaats van 4×5), betere widgetkiezer-preview en behoud van de bestaande scrollbare volledige timerlijst.
- De originele multi-accountwidget, accountnamen, potionrekenwerk en normale meldingen blijven onaangetast.

### EN
- On Fold-sized widgets, reduce each account to just two dense lines: name/builder/helper state above, builder/lab/pet countdowns and finish clock times together below.
- Reduce row height 69dp → 44dp (~36% less), eliminating the redundant per-account boost legend.
- One row on wide widgets ≥665dp; retain a safe three-row fallback only below 325dp.
- Compact calendar dates avoid clock-time truncation, e.g. 9/11 23:55.
- Reduce default size to 4×4, update launcher preview and preserve the complete scrollable upgrade list.
- Keep existing widgets, saved account state, potion forecasts and alarms unchanged.

## Multi-account widget met voorspelde eindkloktijden en visuele Potion-accenten — 6 november 2026

### NL
- De aparte widget is hernoemd van 'All Timers · Test' naar 'Alle timers' (Engels 'All Timers'), zonder test-label in de launcher of widget.
- De bestaande geïnstalleerde provider blijft dezelfde, zodat de oude testwidget automatisch wordt vervangen door het nieuwe ontwerp en de andere widgets behouden blijven.
- Bovenaan een overzicht met een regel per account, in de originele paars-gouden stijl en dezelfde accountvolgorde; buildersstatus, helperstatus en bouwer-, onderzoek- en pet-aftellingen met elk hun eigen eindkloktijd.
- Potion-beïnvloede bouw- en onderzoekstimers tonen een groen bliksemicoon en groene aftel- en eindtijden. Normale eindtijden behouden de oorspronkelijke goud-/lavendeltinten.
- Op brede schermen verschijnt één regel per account; bij smallere formaten worden de velden compact over drie regels weergegeven.
- Scroll naar beneden voor een aparte lijst met iedere individuele upgrade, ook Goblin Researcher, en hun geschatte eindtijd / potionwinst.
- De Sync ON/OFF-balk onderaan is opnieuw klikbaar om de bestaande zwevende syncservice te bedienen.
- Geen wijzigingen aan originele JSON-upgrades, aparte Potion-widgets, normale alarmschema's of app-identiteit.

### EN
- Rename the separate widget from All Timers · Test to All Timers, preserving its receiver and any existing installed widget.
- Restore original purple/gold multi-account appearance and ordering, with builder/helper status and live builder/lab/pet countdowns plus finish clock times for each.
- Use green lightning, green countdown and green finish times for Potion-accelerated upgrades; keep standard colors for unboosted timers.
- Show single-line account rows on wide layouts or readable three-line compact rows on smaller widgets.
- Scroll beyond the account overview to access every individual upgrade, including Goblin Researcher research, sorted by predicted finish.
- Restore interactive Sync ON/OFF footer. Leave other widgets and normal notification schedule untouched.

## Experimentele All Timers-widget met potion-voorspellingen — 6 november 2026

### NL
- Nieuwe zelfstandige, scrollbare 'Alle timers · Test'-widget voor alle actieve upgrades van alle opgeslagen accounts, standaard 4×3 en vrij te vergroten.
- Toon per regel account, upgrade, categorie (bouwers, laboratorium, Goblin Researcher of pets), live resterende tijd, nieuwe eindtijd en de eventuele door 10×/24× potion bespaarde tijd.
- Sorteer alle timers op hun door potions berekende eindtijd; voltooide maar nog niet opnieuw gesynchroniseerde versnellingen blijven als 'Klaar' zichtbaar.
- Een tik op een timer opent het bijbehorende account in ClashTimerHub. De testwidget wijzigt geen bestaande timers of alarmen.
- De JSON-import verwerkt nu, indien aanwezig, ook boosts.builder_boost in seconden om de Builder Potion-aftelling te synchroniseren. Bestaande Research Potion-import via boosts.lab_boost blijft ongewijzigd.
- Ontbrekende/lege boostvelden wissen geen handmatig ingestelde actieve potion.
- De zes bestaande widgets, opgeslagen accounts, meldingen en Android-app-ID blijven intact.

### EN
- Independent, vertically scrollable 'All Timers · Test' widget for every active upgrade across all saved accounts; starts at 4×3 and can be resized.
- Each row shows account, upgrade, category (builders, lab, Goblin Researcher or pets), live countdown, potion-adjusted end time and any 10×/24× time saved.
- Sort by predicted finish; potion-completed timers remain marked Ready until resynced.
- Tap a row to open its account. This experimental widget leaves original upgrade alarms and saved data untouched.
- If an export includes boosts.builder_boost, use its remaining seconds to refresh Builder Potion status, while preserving manual entries when metadata is absent.
- Keep all existing widgets and application identity unchanged.

## Goblin Researcher en actieve Research Potion uit de JSON — 6 november 2026

### NL
- De JSON-import verwerkt nu ook siege_machines als Laboratorium-upgrades. Hierdoor wordt Goblin Researcher-onderzoek, zoals Battle Drill, zichtbaar in de Research-widget, de individuele voorspellingen en de bijbehorende eindtijdmeldingen.
- De markering extra=true blijft bewaard; in het onderzoeksdetail staat dan expliciet Goblin Researcher.
- Het echte veld boosts.lab_boost uit de export bevat resterende onderzoekboost-seconden. Bij een nieuwe sync corrigeert de app hiermee de Research Potion-aftelling en de nieuwe onderzoeks-eindtijden.
- Automatisch verversen van Research Potion-tijd gebeurt alleen bij een geldige, positieve boostduur; ontbrekende of nulwaarden wissen de onafhankelijk bijgehouden boosttijd niet.
- Een onvolledige export zonder siege_machines mag een eerder actief Goblin-onderzoek niet stilzwijgend verwijderen.
- De bestaande Builder Potion, andere timers, accountnamen, normale meldingen en ondertekening blijven intact.

### EN
- Parse siege_machines as Laboratory research, including concurrent Goblin Researcher upgrades such as Battle Drill. Research widgets, per-upgrade forecasts and completion notifications now see these timers.
- Preserve the extra=true flag and explicitly label Goblin Researcher in the research detail view.
- Reconcile independent Research Potion countdowns using positive boosts.lab_boost seconds and the original export timestamp, without resetting missing/zero values.
- Prevent partial exports lacking siege_machines from silently erasing an existing Goblin Researcher timer.
- Keep Builder Potions, other account data, normal notifications and the signing identity unchanged.

## Potion-aftellingen blijven onafhankelijk na synchronisatie — 6 november 2026

### NL
- Actieve Builder- en Research Potion-timers blijven zichtbaar en tellen verder na iedere JSON-sync; geen uitroepteken meer doordat de importtijd verandert.
- Potions bewaren hun echte start- en eindmoment per account. Een nieuwe sync wist of herstart deze niet.
- Nieuwe JSON-tijden worden het uitgangspunt voor de upgradevoorspellingen; alleen daarna relevante boosttijd telt nog mee. Oude, reeds verbruikte boosttijd wordt niet dubbel toegepast.
- De bestaande sync plant de bijbehorende voorspelde gereed- en voorafmeldingen opnieuw. Gewone upgrade-timers en alarmen worden niet veranderd.
- Alleen echt onleesbare opgeslagen Potion-gegevens geven nog een waarschuwing.

### EN
- Builder and Research Potion countdowns continue across JSON imports without sync-induced exclamation marks.
- Real-world boost start/end times remain independent per account and are never erased or restarted by sync.
- Forecasts use the latest imported upgrade finish times and apply only relevant remaining boost, not past boost time already consumed.
- Existing sync scheduling updates boosted completion/prewarning alarms without changing ordinary upgrade notifications.
- Warn only if saved Potion data genuinely cannot be read.

## Alle meldingen in één venster met twee potion-opties — 6 november 2026

### NL
- Samengevoegde meldingsinstellingen: verwijder het aparte Potion-venster en de Potion-alarmtest.
- Twee opties voor zowel Builder als Research Potions: X minuten voor de aangepaste eindtijd (0 = uit) en melding op de aangepaste eindtijd.
- Vertalingen Nederlands/Engels toegevoegd. Back-ups bewaren beide instellingen en oudere back-ups blijven leesbaar.

### EN
- Merge notification preferences into one dialog and remove separate Potion settings and test.
- Add only two settings for both Potion types: X-minute warning before boosted upgrade completion (0 = off) and notification at boosted completion.
- English/Dutch translations and backup compatibility are preserved.

## Instelbare potion-beïnvloede upgrade-eindtijdmeldingen — 6 november 2026

### NL
- Eigen instellingen 'Potion-meldingen instellen', volledig onafhankelijk van gewone upgrade-alarmen.
- Zet potion-upgrades gereed aan of uit, afzonderlijk voor Builder en Research, en per opgeslagen account.
- Melding alleen op de door potions herberekende eindtijd: geen melding wanneer de potion afloopt en geen voorafmelding.
- Potionalarmen worden bij sync of het aanpassen van de boosttijd opnieuw gepland en gecontroleerd voor verzending.
- Test het aparte Potion-alarmkanaal met de nieuwe test over één minuut, inclusief melding bij ontbrekende exacte alarmtoestemming.
- Potionalarmvoorkeuren blijven behouden na sync en maken deel uit van lokale/Drive-back-ups en herstel; oude back-ups blijven compatibel.
- Oorspronkelijke JSON-timers en gewone meldingen blijven intact.

### EN
- Separate Potion completion settings, independent from ordinary upgrade alarms.
- Toggle Potion completion alerts globally, by Builder/Research type and by account.
- Notify only when the potion-recalculated upgrade actually reaches its finish time, with no expiry reminder or early warning.
- Reschedule and validate potion alarms after sync or boost adjustment.
- Verify the dedicated notification channel using a one-minute Potion alarm test.
- Back up and restore potion notification preferences without breaking old backups.
- Preserve regular imported timers and notifications.

## Een-tik Sync zonder game te verlaten — 6 november 2026

### NL
- Een enkele tik op de zwevende Sync-knop leest de Clash-JSON zonder een Activity te openen of van de game weg te schakelen.
- De knop krijgt uitsluitend tijdens een expliciete sync heel kort focus voor de Android-klembordbeveiliging.
- Direct na afloop of een fout wordt FLAG_NOT_FOCUSABLE hersteld, zodat Clash-chat zijn toetsenbord kan gebruiken.
- Bij weigering van klembordfocus blijft Clash open en verschijnt een foutmelding in de zwevende feedback.
- Behoud dubbel tikken voor More Settings, het verslepen van de knop en bestaande accounts/timers.

### EN
- One-tap floating Sync reads Clash JSON without launching an Activity or switching away from the game.
- The small bubble temporarily requests input focus only while importing the clipboard.
- Restore FLAG_NOT_FOCUSABLE after success, failure or timeout to preserve Clash chat keyboard.
- On denied focus, keep Clash open and show overlay feedback.
- Preserve double-tap More Settings, dragging and all account/timer data.

## Herstel zwevende Sync-knop en Android klembordfocus — 6 november 2026

### NL
- Herstel de een-tik JSON-import vanuit de zwevende Sync-knop op Android 10+ en Samsung One UI.
- De altijd-zichtbare knop blijft niet-focusbaar zodat Clash-chat het toetsenbord kan openen.
- Een tik opent heel kort de bestaande QuickImportActivity met een klein transparant venster; importeren gebeurt pas na de echte Android focusmelding.
- Het importresultaat verschijnt opnieuw op de paarse zwevende knop (groen bij succes, rood bij fout), waarna Clash weer zichtbaar is.
- Beperk het wachten op focus en herprobeer tijdelijk lege klembordresultaten kort. Toon een duidelijke foutmelding als de klembordinhoud echt niet beschikbaar is.
- Dubbel tikken voor Data Export, het verslepen van de knop, zeven accountgegevens en alle timers blijven behouden.

### EN
- Restore single-tap JSON import from the floating Sync button on Android 10+ and Samsung One UI.
- Keep the persistent bubble non-focusable to avoid blocking the Clash chat keyboard.
- A tap briefly opens a tiny transparent QuickImportActivity and only reads the clipboard once Android grants input focus.
- Report success or failure on the floating bubble and return straight to Clash.
- Bound focus waiting and retry transient null clipboard reads; show helpful errors.
- Preserve double tap for Data Export, dragging and all account/timer data.

## Uniforme Research Potion-widget zonder flask-icoon — 6 november 2026

### NL
- De Research Potion-widget gebruikt nu dezelfde paars-gouden styling als de Builder Potion-widget, ook in de widgetkiezer.
- Het 🧪-icoon is verwijderd uit de Research-widgettitel, lege booststatus en statische voorbeeldweergave.
- De herkenbare titel Research Potion · 24× en de bestaande +1u-knoppen, timers, meldingen en accountvolgorde blijven behouden.
- Builder Potion- en reguliere widgets zijn ongewijzigd.

### EN
- Research Potion now uses the same purple-and-gold styling as Builder Potion, including its widget picker preview.
- Removed the flask emoji from the Research header, idle timer state and static preview.
- Research Potion · 24× remains distinguishable by text; +1h buttons, forecasts, notifications and account order are preserved.
- Builder Potion and other widgets are unchanged.

## Compacte Potion-widgets met herkenbare namen — 6 november 2026

### NL
- Beide Potion-widgets zijn standaard 4×2 in plaats van 4×4; rijhoogte 22dp, compactere kop- en voettekst.
- In de widgetkiezer verschijnen aparte labels: Builder Potions en Research Potions.
- Beide hebben een eigen statische voorbeeldweergave met herkenbare Builder- of Research-titel en voorbeeldaccounts.
- De Research-widget toont ook voor de eerste update de correcte 24×-titel.
- Bestaande +1u-knoppen, eindtijdvoorspellingen, accountherkenning en boostmeldingen blijven werken.
- Een al geplaatste widget kan handmatig verkleind of opnieuw geplaatst moeten worden; de launcher bewaart de toegewezen ruimte.

### EN
- Both Potion widgets now default to 4×2 rather than 4×4, with 22dp rows and shorter header/footer.
- Widget picker displays distinct Builder Potions and Research Potions labels and previews.
- Research widget now shows 24× in its initial layout as well as its live view.
- Per-account +1h actions, forecasts and notifications are preserved.
- Existing widgets may need to be resized or re-added because the launcher controls their reserved space.

## Research Potions, boostmeldingen en toetsenbordfix — 6 november 2026

### NL
- Zesde aparte multi-accountwidget voor Research Potions: 24× Laboratory-snelheid gedurende één uur, maximaal 23 uur tijdwinst; +1u of exacte resterende boosttijd per account.
- Research-overzicht toont de voorspelde eindtijden van alle actieve Laboratory-upgrades naast de oorspronkelijke tijden.
- Afzonderlijke meldingen wanneer een door Builder Potions of Research Potions versnelde upgrade volgens de voorspelling gereed is.
- Voorspelde meldingen zijn account- en upgradegebonden, worden opnieuw berekend na wijziging of sync, en controleren bij afgaan of de oorspronkelijke timer en boostvoorspelling nog kloppen.
- Bestaande JSON-timers en hun oorspronkelijke alarmen blijven ongewijzigd.
- De zwevende Sync-knop gebruikt voortaan een niet-focusbaar overlayvenster, zodat deze de invoerfocus en het Android-toetsenbord van Clash-chat niet meer mag overnemen.

### EN
- Sixth standalone multi-account widget for Research Potions: 24× Laboratory speed for one hour, up to 23 hours saved; +1h or precise remaining duration per account.
- Research overview shows forecast completion of every active Laboratory upgrade alongside the original times.
- Separate notifications when builder or research upgrades are expected to finish early due to potions.
- Forecast notifications are account/upgrade scoped, rescheduled on edits and syncs, and validated against the current JSON and boost projection before posting.
- Original imported timers and their notifications are preserved.
- The floating Sync overlay is now non-focusable so it cannot steal the game's chat input focus and soft keyboard.

## Duidelijkere Builder Potion-bediening — 6 november 2026

### NL
- De Builder Potion-widget gebruikt nu dezelfde accountvolgorde als de bestaande multi-accountwidget, inclusief sortering op eerstvolgende bouwer of een ingestelde eigen volgorde.
- De +1u-knop heeft een contrastrijke achtergrond en een groter aanraakvlak. Tikken toont ook kort een bevestiging.
- De nieuwe eindtijd toont, waar er een versnelling is, ook de exacte geschatte tijdwinst (bijv. 02:45 −8h34).
- De knop rechtsboven opent voortaan een bruikbaar overzicht met alle accounts, booststatus, voorspelde bouwer-eindtijd en afzonderlijke +1u-knoppen.
- Compacte, duidelijk gekleurde knoppen in het boost-bewerkscherm, een terugknop en correcte afstand tot Android-status- en navigatiebalk.
- Toon per bouwer de voorspelde eindtijd, oorspronkelijke eindtijd en werkelijke berekende tijdwinst; gelijknamige upgrades krijgen een volgnummer.
- Verduidelijk waarom één uur 10×-boost soms minder dan negen uur tijdwinst geeft: de upgrade eindigt dan al binnen de potion.

### EN
- Builder Potion widget now follows the original multi-account widget's sorting, including next-builder or configured custom order.
- The +1h button is larger and more prominent, with a short tap confirmation.
- Forecasted finish time also shows the estimated time saved, e.g. 02:45 −8h34.
- Top-right menu opens a useful all-account overview with per-account potion status, predicted builder finish and one-tap +1h actions.
- Compact, high-contrast controls in the boost editor, an all-accounts back button and proper system-bar insets.
- Each builder shows predicted and original completion with actual time saved; duplicate upgrade labels get ordinal suffixes.
- Explain why a one-hour 10× boost saves less than nine hours when an upgrade finishes during the potion.

## Builder Potion multi-accountwidget — 6 november 2026

### NL
- Vijfde optionele widget op basis van de bestaande accountgegevens: toont per account de resterende Builder Potion-boost en de nieuwe geschatte eindtijd van de eerstvolgende bouwer.
- Tik op +1u bij een account zodra je in Clash een Builder Potion activeert; iedere extra potion voegt één uur aan de boost toe. Er is bewust geen +10m-knop.
- Tik op de accountnaam of tijd om exacte resterende uren en minuten in te voeren, de boost te wissen en alle geschatte bouwer-eindtijden naast de oorspronkelijke tijden te zien.
- Alle vier bestaande widgets, originele JSON-timers, meldingen, accounts en synchronisatie blijven intact.
- De boostvoorspelling wordt na een nieuwe JSON-import uitgeschakeld totdat de boost opnieuw wordt bevestigd; daardoor wordt de bestaande export niet stilzwijgend dubbel versneld.
- De nieuwe widget werkt zonder schermbediening in Clash of extra Android-toestemmingen.

### EN
- Fifth optional widget based on existing account data, showing each account's remaining Builder Potion boost and the estimated earlier next builder completion.
- Tap +1h when you activate a Builder Potion in Clash; each additional potion extends the boost by one hour. No irrelevant +10m action.
- Tap an account or timer to enter exact hours and minutes, clear a boost and inspect every predicted completion next to its original time.
- All four existing widgets, imported JSON timers, alerts, accounts and syncing stay unchanged.
- A new JSON import invalidates previous boost forecasts until the duration is confirmed again, avoiding silent double application.
- The new widget needs no automated Clash interactions or additional Android permissions.

## Testrelease automatische updatecontrole — 5 november 2026

### NL
- Deze testrelease bevat geen nieuwe appfunctionaliteit ten opzichte van build #474.
- Publicatie van een hoger buildnummer maakt het mogelijk om de automatische melding in build #474 op een telefoon te testen.
- Start build #474 opnieuw: de app moet build #475 aanbieden, zonder handmatig naar Instellingen te gaan.
- Kies 'Later', verlaat de app en open haar opnieuw om te controleren of de melding opnieuw verschijnt.
- Installeer daarna via de melding en controleer de succesbevestiging en het behoud van accounts en timers.

### EN
- This is a test release with no additional application features compared with build #474.
- The higher published build number enables real-device testing of the update prompt in build #474.
- Reopen build #474: it should offer build #475 without manually visiting Settings.
- Tap 'Later', leave the app and reopen it to verify the prompt returns on the next foreground visit.
- Install from the prompt and verify the success confirmation and preservation of accounts and timers.

## Betrouwbaardere automatische app-updates — 5 november 2026

### NL
- Controleer bij ieder nieuw bezoek aan de app op GitHub-updates; de wachttijd van zes uur vervalt.
- Na 'Later' kan een nieuwere build bij de volgende appstart opnieuw worden aangeboden, zonder herhaalde meldingen in dezelfde sessie.
- Een handmatige controle tijdens een lopende automatische controle geeft alsnog een expliciete uitkomst.
- Voorkom gelijktijdige updatevensters, en onderbreek geen actieve download, installatie of toestemmingswizard.
- Toon na een geslaagde update de bevestiging met het geïnstalleerde buildnummer, ook als Android de app tijdens installatie vervangt.
- Updates blijven optioneel: APK-controles, bestaande ondertekening, accounts en Android-installatiebevestiging blijven behouden.

### EN
- Check GitHub for app updates at each new foreground visit, without the old six-hour delay.
- After 'Later', offer a newer build again at the next app launch, without repeated prompts in the same visit.
- An explicit check while an automatic request is running still shows a result.
- Prevent duplicate prompts and do not interrupt an ongoing download, installation or permission wizard.
- Show the installed build confirmation after a successful update, even if Android replaces the app process.
- Updates stay optional: APK verification, signing, accounts and Android installation confirmation remain unchanged.

## Sneller exporteren vanuit Clash — 5 november 2026

### NL
- Dubbel tik op de zwevende sync-knop om Clash of Clans direct naar Meer instellingen te laten navigeren, als het spel de deeplink ondersteunt.
- Eén tik blijft de bestaande JSON-import uitvoeren; lang indrukken en verslepen blijven werken.
- De instructie bij de zwevende knop is bijgewerkt.
- Bij onvoldoende deeplinkondersteuning verschijnt een instructie om de export handmatig te openen.
- Er worden geen screenshots, AI-modellen of extra toestemmingen gebruikt.

### EN
- Double tap the floating sync button to ask Clash of Clans to open More Settings directly when the game supports the deep link.
- Single tap still imports the copied JSON; long press and dragging continue to work.
- Updated the floating button's instructions.
- If the game does not support the deep link, manual export instructions are shown.
- No screenshots, AI models or additional permissions are involved.

## Laboratory-icoon volgens gebruikersvoorkeur — 5 november 2026

### NL
- Gebruik opnieuw het gekozen detective-icoon 🕵️ voor Laboratory in de app en alle oorspronkelijke widgets.
- Behoud de compacte statusregels en overige verbeteringen van de vorige build.

### EN
- Use the chosen detective icon 🕵️ for Laboratory in the app and all original widgets.
- Keep the compact status labels and other improvements from the previous build.

## Duidelijke upgrade-iconen — 5 november 2026

### NL
- Vervang verouderde wizard- en laboratoriumsymbolen door het duidelijke 🧪-icoon in Overview, accountdetails en de oorspronkelijke widgets.
- Maak de statussen voor bouwers, Lab en Pets op accountkaarten korter, zodat ze minder snel afbreken.
- Laat bij volledig geüpgradede Lab- of Pet-onderdelen een vinkje zien in plaats van het lange MAX-label.
- Bestaande accounts, upgradegegevens, timerberekeningen en vier widgets blijven behouden.

### EN
- Replace old wizard and laboratory symbols with a clear 🧪 icon in Overview, account details and the original widgets.
- Shorten the Builders, Lab and Pets statuses on account cards to reduce clipping.
- Show a check mark instead of the long MAX label for fully upgraded Lab or Pets.
- Preserve existing accounts, upgrade data, timer calculations and the four original widgets.

## Compacte instellingen — 5 November 2026

### NL
- De instellingenpagina is compacter: kleinere kaarten en rijen, minder witruimte en geen overbodige dubbele koptekst.
- Op brede schermen en de uitgeklapte Fold verschijnen de instellingen in twee overzichtelijke kolommen.
- De dubbele back-upactie 'Opslaan als bestand' is verwijderd. 'Opslaan in Google Drive' blijft beschikbaar om een JSON-bestand te delen.
- Back-ups kopiëren, herstellen en importeren blijven ongewijzigd.

### EN
- The Settings screen is more compact, with smaller cards and rows, less wasted space and no redundant large header.
- On wider screens and the unfolded Fold, Settings now uses two organized columns.
- Removed the redundant 'Save as file' backup option. 'Save to Google Drive' remains available to share a JSON file.
- Copy, restore and import backup features remain unchanged.

## Widgetbeheer en update-feedback — 5 November 2026

### NL
- Widget Studio volledig verwijderd; de vier oorspronkelijke widgets blijven behouden.
- Terug vanuit het widgetbewerkingsscherm via het tandwiel opent de app bij Instellingen.
- Na een succesvolle app-update verschijnt bij de volgende appstart een bevestiging met het buildnummer.
- De lijst met nieuwe functies en verbeteringen verschijnt in de gekozen app-taal.

### EN
- Widget Studio has been removed completely; the four original widgets remain available.
- Back from the widget editor opened via the gear now returns to the app's Settings screen.
- After a successful app update, the next app launch shows confirmation with the installed build number.
- The list of new features and improvements appears in the language selected in the app.

## 7.5.9-beta1 — 4 November 2026

### Helperkolom op dezelfde regel — bredere multi-accountwidget
- Helpers staan nu direct **na het aantal builders** en **vóór de 🧙 laboratoriumtimer** op dezelfde rij.
- De losse tweede regel met helperstatus is volledig verwijderd; ieder account gebruikt weer 24dp hoogte, zodat zeven accounts onder elkaar kunnen blijven staan.
- De helperkolom krijgt, net als de andere kolommen, een gezamenlijke breedte op basis van de langste helperstatus. Zo beginnen de 🧙 lab-, 🐾 pet- en bouwtimers op dezelfde horizontale positie bij ieder account.
- De volledige overzichtswidget blijft in deze versie altijd **één regel per account**; smalle formaten schakelen niet meer over naar twee of meer regels.
- De standaardwidget wordt breder geplaatst (doel: vier Android-launchercellen); reeds bestaande widgets kun je handmatig horizontaal verbreden.
- Belangrijk: als de ingestelde widgetbreedte te klein is voor alle kolommen, kan Android inhoud rechts afsnijden. Vergroot de widget via het startscherm; Android staat niet toe dat de app een bestaande widget zelf groter maakt.
- De instelling **Helpers tonen** per widget, de Alchemist-vinkjes, bestaande timers, data en back-ups blijven ongewijzigd.


## 7.5.8-beta1 — 4 November 2026

### Alchemist per account — standaard uit
- **Alchemist beschikbaar** is een accountgebonden selectievakje onder de helpersectie, net als de bestaande MAX-vinkjes.
- De Alchemist is standaard uitgeschakeld voor alle accounts, ook als de Clash-export helperdata bevat.
- Alleen wanneer **Alchemist beschikbaar** voor een account is aangevinkt, wordt deze helper getoond en meegerekend in de Helper Tracker en multi-accountwidget.
- Builder's Apprentice en Lab Assistant blijven zichtbaar volgens de beschikbare exportgegevens.
- Wisselen van de instelling werkt direct de widget bij. De helperdata blijven opgeslagen, zodat terugschakelen geen nieuwe import vereist.
- De instelling wordt meegenomen in JSON-back-ups en bij koppelen van player tags aan bestaande accounts. Eerdere back-ups en timers blijven bruikbaar.


## 7.5.7-beta1 — 4 November 2026

### Helper Tracker — automatisch bij volledige Clash Sync
- Lees `helpers` uit de volledige Clash-village JSON per gekoppelde player tag.
- Bewaar helper-ID, level, tijdstip van de export en beschikbare `helper_cooldown`-tijden.
- De accountdetailpagina toont iedere beschikbare helper en bijbehorend level, beschikbare cooldown of expliciet 'onbekend' als de export geen timer bevat.
- Helpernamen: Builder's Apprentice / Bouwleerling, Lab Assistant / Labassistent en Alchemist. Onbekende IDs blijven zichtbaar met hun ID, zonder een naam te gokken.
- De multi-accountwidget krijgt onder accounts met helperdata een compacte statusregel, bijvoorbeeld `🤝 1/3 · 4h15`. De helperregels kunnen per widget worden uitgezet via het tandwiel.
- De widget berekent de benodigde extra hoogte per account en toont op kleinere formaten het aantal rijen dat werkelijk past.
- Helpergegevens en de widgetvoorkeur worden opgenomen in JSON-back-ups; oude back-ups blijven herstelbaar.
- Helpers ontbreken in sommige exports. Dan blijven bestaande helpergegevens bewaard en komen er geen fictieve helper-timers bij.
- Er worden geen potionvoorraden afgeleid en bestaande bouw-/lab-/pettimers worden niet aangepast. Er is geen apart JSON-inspectorscherm.

### Testen
- Controleer op het toestel of de exports `helpers` en `helper_cooldown` bevatten en of de One UI-widget alle gewenste accounts toont bij de gekozen hoogte.


## 7.5.6-beta1 — 4 November 2026

### Unieke back-upbestandsnamen
- Exports krijgen standaard de lokale datum, tijd en milliseconden: `ClashTimerHub-backup-YYYY-MM-DD_HH-mm-ss-SSS.json`.
- Dit geldt zowel voor **Opslaan in Google Drive** (via Delen) als **Opslaan als bestand**.
- Ook meerdere back-ups op dezelfde dag (zelfs binnen een seconde) zijn zo aan de naam te onderscheiden.
- JSON-indeling en import blijven ongewijzigd; bestaande account- en timergegevens blijven behouden.
- Google Drive upload via Delen vervangt nog steeds geen bestaand bestand automatisch.


## 7.5.5-beta1 — 4 November 2026

### Back-up — datum in bestandsnaam
- Nieuwe JSON-back-ups heten standaard `ClashTimerHub-backup-YYYY-MM-DD.json`, gebaseerd op de datum van de telefoon.
- De datum wordt gebruikt voor zowel **Opslaan in Google Drive** (Android Delen) als **Opslaan als bestand** (bestandskiezer).
- Bestaande back-ups blijven importeerbaar. Google Drive kan nog steeds meerdere bestanden met dezelfde naam bewaren; automatisch overschrijven wordt niet beloofd.

### APK Update Manager — betrouwbaarheid en herstel
- De actieve APK-download (DownloadManager-ID, buildnummer, bestandslocatie en SHA-256) blijft bewaard wanneer de app naar de achtergrond verdwijnt of opnieuw wordt geopend.
- Voltooide downloads worden bij terugkeer opnieuw gecontroleerd en daarna via Androids PackageInstaller aangeboden.
- Tijdens een open app luistert de updater naar downloadafrondingen; downloaden/controle blijft doorgaan wanneer de app tijdelijk niet zichtbaar is.
- Bij een vastgelopen of mislukte download verschijnt een concrete fout of Android-redencode.
- Verificatie blijft verplicht: downloadcontrole, juiste app-ID, nieuwer versionCode en dezelfde APK-ondertekening.
- Bij een installatiefout met een eerder **geverifieerde** APK kan de gebruiker **Via Android installeren** kiezen als alternatieve methode. Ook die methode controleert alles opnieuw.
- Nieuw: **Instellingen → App-updates → Update-diagnose** met installed/latest build, installatietoestemming, actieve download, beschikbare APK en opgeslagen fout.
- De alternatieve route gebruikt een tijdelijke, beveiligde FileProvider-toegang; de app vraagt nooit om je bestaande installatie te verwijderen.

### Beperkingen
- Android vereist nog altijd een bevestiging van de gebruiker om een APK te installeren.
- Buildsucces op GitHub bewijst niet dat de installatie op Samsung One UI is gelukt; daarvoor is een toesteltest nodig.


## 7.5.4-beta1 — 4 November 2026

### Widget finish label correction
- English next-day dates now show **Tmr** instead of **Tom**, for example `Tmr 00:22`.
- Dutch **Mrg** remains unchanged; the shared widget column adjusts to the displayed text.


## 7.5.3-beta1 — 4 November 2026

### Compact widget finish labels
- **Tomorrow → Tom:** The next-day finish label is now three letters in English, e.g. `Tom 00:22`.
- **Morgen → Mrg:** The Dutch equivalent also uses three letters, e.g. `Mrg 00:22`.
- The **shared column width is recalculated from the abbreviated display string** so the multi-account widget can fit into narrower widths before switching to multiple lines.
- Today, dates beyond tomorrow, timers, Sync controls, and the widget's other settings are unchanged.


## 7.5.2-beta1 — 4 November 2026

### Back-up — Google Drive without Google Cloud
- **Opslaan in Google Drive:** Creates a real, portable `ClashTimerHub-backup.json` file and opens Android's sharing sheet. Choose the Google Drive app to upload it directly to My Drive. No Downloads file picker, Google Cloud or OAuth registration required.
- **Opslaan als bestand:** Uses Android's Save As document picker for a standalone .json backup on the device or a compatible cloud document provider.
- **Back-upbestand importeren:** Choose a previously saved JSON file and confirm restoration of accounts, timers and settings. The existing clipboard backup remains available.
- The previous direct OAuth Drive controls were replaced by this simpler user-controlled flow. **Drive uploads via sharing are manual**; they do not automatically replace an existing file or upload after every sync.
- Existing backup JSON schema, data, widget settings and local timers remain compatible.

### Widget — compact all-account layout
- Remove the top title/header entirely to recover vertical space for account rows.
- Move the settings gear to the bottom-right corner, beside a large always-visible Sync ON/OFF control.
- Recalculate account row capacity without the former header allocation. The number of visible accounts still depends on the physical widget height and whether the data fits in one or multiple lines; no text is shortened to force additional rows.
- Preserve transparent backgrounds, aligned columns, category icons and text sizes.


## 7.5.1-beta1 — 4 November 2026

### Native Google Drive backups
- **Google account authorization:** Connect ClashTimerHub to My Drive through Google Identity Services (limited `drive.file` access; no full-Drive permissions or access to your Google password).
- **Visible JSON file:** Create `ClashTimerHub-backup.json` directly in My Drive using the Google Drive API. No Android Downloads folder, no Storage Access Framework location picker.
- **Replace previous file:** Select the already active backup or choose another app-accessible JSON file and overwrite it in My Drive after confirmation.
- **Restore from Drive:** View eligible backup files in a list, choose one, download and validate its JSON, then confirm restoration of timers and account settings.
- **Optional automatic updates:** Select a backup as the auto-sync target; after successful Clash sync, refresh that file when Google authorization can be renewed without user interaction.
- **Disconnect:** Revoke granted Drive authorization, clear local selected-file references, leave the Drive files intact.
- **Safety:** The existing local / clipboard backup format and data remain unchanged; no Drive file is overwritten without user confirmation except the specifically selected automatic backup destination.

### Required one-time Google Cloud setup
- Enable Google Drive API, configure OAuth consent, and register an **Android OAuth client** with package `nl.clashtimerhub.app` and the SHA-1 certificate fingerprint of the actual GitHub APK signing key.
- Without this registration, the Google authorization screen can refuse the connection even if Android APK compilation succeeds.
- The limited `drive.file` permission can list files created or explicitly authorized for this app; unrelated existing Drive files cannot be browsed automatically. You can still restore from existing backups made by ClashTimerHub once accessible.


## 7.5.0-beta1 — 4 November 2026

### Google Drive backup manager
- **Save to Google Drive:** In Settings > Back-up, select Google Drive in Android's file picker and create a `ClashTimerHub-backup.json` document. The app keeps the document permission for future backups without a Google password or broad account access.
- **Automatic after successful Sync:** After a completed clipboard/game-export sync or accepted screenshot OCR scan, the app writes a new snapshot to the selected document if Auto backup is enabled. A Drive failure never causes an otherwise successful Clash sync to fail.
- **Back up now:** Manually overwrite the selected backup file with the latest accounts and timers.
- **Restore from Google Drive:** Select any existing ClashTimerHub JSON backup via Android's file picker. The app validates the content and asks for confirmation before replacing current accounts and timers.
- **Visible status:** Shows the linked file, whether Auto backup is enabled, the last successful write time, and the latest error.
- **Disconnect:** Forget access to the linked document and disable automatic backups; do not delete the file.

### Data and compatibility
- Preserves existing v1 JSON format and previous clipboard backups.
- Includes player tags, account names, active upgrades, MAX flags, notification settings, language, screenshot timer calibration and all installed widget presets.
- On a new device widget IDs are assigned by Android, so the widgets themselves may need to be added first.
- Backup uses Android's Storage Access Framework, not a direct OAuth Drive integration. The user chooses Google Drive (or another document provider) in the picker; Drive may sync a selected file to the cloud asynchronously.
- The Android provider's write permissions and online availability are required for subsequent automatic backups; status shows failures, and the app retains an extra last-backup copy in app-private storage.


## 7.4.8-beta1 — 4 November 2026

### Widget improvements
- **Sync widget is configurable:** Use its new gear to open that widget instance's settings and change its background independently. Tapping the rest of the widget still toggles floating Sync.
- **Sync widget setup:** Android's widget picker can launch configuration when adding a new Sync widget.
- **Single-account widget can be shorter:** Reduced the minimum and resize heights, targets a 3-column by 2-row home-screen widget rather than requiring four vertical cells.
- **Compact layout:** Reduced account widget padding and row heights while preserving builders, lab, pets, finish times and Sync status.
- Existing account data, timers and other widget settings stay intact.

### Testing
- Exact home-screen cell sizing depends on the Samsung One UI launcher. If an existing widget remains four rows high after updating, try resizing it or removing and re-adding only the widget (not the app).


## 7.4.7-beta1 — 4 November 2026

### Widget settings editor
- Fixed the widget configuration screen overlapping Android's status and navigation bars, particularly on Samsung One UI with edge-to-edge layouts.
- Added a fixed header naming the widget type and ID, a scrollable settings area, and an always-visible Save widget button.
- Existing widget instances retain their own account selection, ordering and appearance settings.
- The multi-account widget has account selection, manual ordering, sorting, text size, lab/pet visibility and background controls.
- Individual account widgets have account, text size, lab/pet visibility and background settings.
- Mini widgets have account, text size and background settings.
- Sync widgets have a background setting and an explanation of their app-wide Sync behavior.
- The Settings widget chooser now identifies each widget by its type, chosen account and widget ID.
- Existing widget preferences and timer data are preserved.

### Verification
- Android APK is checked by GitHub Actions. UI interactions and layout still need testing on Samsung One UI.


## 7.4.6-beta1 — 4 November 2026

### New
- **One guided permission setup:** On first normal app launch, ClashTimerHub offers one clear step at a time for notifications, display-over-other-apps, exact alarms and installation of APK updates.
- **Return and continue:** After granting a special Android permission in system Settings, return to ClashTimerHub and the wizard proceeds to the next step.
- **Check existing permissions:** Already-granted steps are skipped, and the wizard shows the number of active permissions.
- **Consent and control:** Every permission has Skip and Later options. Settings > Permissions check lets users open the flow again at any time.
- **No repeated startup prompts:** Once completed, skipped or postponed, the wizard will not appear automatically on every launch.

### Important
- Android does not allow all four permissions to be granted by a single tap. Each system confirmation remains the user's choice.
- The overlay Sync button remains opt-in; granting its permission does not enable it.
- Permissions are specific to the Android device and are not granted by backup/restore. Existing accounts and timers are unchanged.


## 7.4.5-beta1 — 4 November 2026

### Widget polish
- Remove the crossed-swords decoration from the multi-account title and Sync labels.
- Remove the top-right total-account and ready-builder summary; keep the Settings gear.
- Replace the indistinct purple research symbol with a recognizable wizard 🧙 in multi-account, compact and single-account widgets.
- Right-align finish times within their shared column so all finish times end at the same position, while retaining full text.
- Reduce horizontal spacing and keep a common shared column grid for all accounts.
- Make the multi-account, single-account and mini cards content-height instead of filling a tall block of empty background.

### Notes
- The visible account count in a compact multi-widget still depends on its actual available height. Increase the widget height to show more accounts when it reflows to multiple lines.
- Successful Android build does not replace hands-on Samsung One UI testing; local data and user settings stay intact.


## 7.4.4-beta1 — 4 November 2026

### Fixed
- **Uniform columns across accounts:** all seven account rows now use the same shared starting position for each successive column. Each column's width is calculated from the largest full content in that column, rather than from each account's individual string length.
- **No shortening or abbreviations:** remove timer ellipses, remove name width clipping and use complete "Morgen"/"Tomorrow" labels instead of Tmr/Mrg.
- **Start-aligned countdowns:** countdown and finish labels begin at the common column boundary instead of right-aligning within variably sized fields.
- **Responsive widths:** only when the combined full columns cannot fit does the widget switch to multi-line account rows, without shrinking any text.
- **Existing settings:** transparent theme, colors, MAX checkmarks, account sorting and timers remain unchanged.

### Verification
- GitHub Actions compiles/signs the Android APK. Visual validation on Samsung One UI remains necessary.


## 7.4.3-beta1 — 4 November 2026

### Fixed
- **Compact account names:** Account-name cells now use only their required width (up to a reasonable maximum); builder status and timers follow immediately instead of leaving a large gap after the username.
- **Never abbreviate countdowns:** Allocated timer widths are preserved. The laboratory, pet, builder countdown and finish time are displayed in full rather than clipped with `...`.
- **Responsive layout:** The widget uses one line per account when wide enough, two lines on medium widths, and three or five lines on narrow launcher widths instead of shrinking timer fonts.
- **Readability:** The active font-size preference remains in effect while resizing.
- **Preserved behavior:** Existing accounts, countdowns, MAX checkmarks, widget sorting and transparency settings are not changed.

### Known limitation
- The amount of information that fits within a fixed widget height depends on available screen space. Make the widget taller on narrow layouts to show all seven accounts.
- Android widget dimensions are launcher-controlled; exact appearance still requires validation on Samsung One UI.


## 7.4.2-beta1 — 4 November 2026

### Fixed
- APK updates now use Android's official PackageInstaller session instead of forwarding an unverified DownloadManager content URI.
- Before installing, verify the APK is complete, matches the expected GitHub SHA-256 digest, uses ClashTimerHub's package ID, has a higher versionCode, and is signed with the installed app's certificate.
- Installation consent is shown by Android. If 'Install unknown apps' permission is missing, guide the user to enable it for ClashTimerHub.
- If an installation fails, show a useful reason (e.g. signature conflict, corrupted APK, incompatible Android version, blocked permission, or insufficient storage) instead of leaving only the generic 'App not installed' message.
- Existing accounts, timers, widgets and preferences are never deleted as part of this update.

### Note
- A build that passes GitHub Actions may still be incompatible with an already installed APK if an older build uses a different signing key. The app now detects that conflict before attempting to install.
- Android installation confirmation is still mandatory; silent installs are unsupported.


## 7.4.1-beta1 — 4 November 2026

### Fixed
- **Consistent widget text size:** resizing to narrower home-screen columns no longer reduces fonts to 8sp; the app uses a readable two-line row for narrower widths instead. For short widget heights, fewer rows may fit; increase widget height to display every account.
- **Update popup:** real line breaks, short release notes without raw Markdown and a compact up-to-date message.
- **Settings backup:** account timers, display names and MAX statuses are now joined by language, OCR timer calibration, notification preferences and widget settings.
- Old v1 backups still import. Widget presets are matched to currently installed widgets of the same type and order; a missing widget must be added to the launcher first.

### Safety
- Existing settings continue to persist through app updates without using backup/restore.
- Android still requires confirmation before installing updates.


## 7.4.0-beta1 — 4 November 2026

### New
- **Updates without ZIP files:** GitHub automatically publishes every successful APK build as a public Release.
- **In-app update checks:** ClashTimerHub reads the latest GitHub Release directly; no extra feed, GitHub login, or API token is required.
- **One-tap update start:** compare installed and latest build numbers, read release notes and download the APK from the app. Android still requires the user's confirmation for installation.
- **Automatic reminder:** on opening the app, check at most once every six hours and offer a newer published build once per release.
- **Fallback:** Settings links directly to the latest GitHub Release, never the Actions ZIP artifact.

### Safety
- The installer updates the existing app; local accounts and timers are not deleted.
- The build workflow refuses to build if the previous signing key is missing.
- GitHub must finish a successful build before the new APK is published.


## 7.3.1-beta1 — 4 November 2026

### Fixed
- Multi-account widget supports a **two-column** home-screen size with a two-line account row. All the key information remains available: account name, builder status, lab, pets, next countdown, finish time.
- Widget Studio **Comfort/Large** text now enlarges the account name and main countdown rather than every secondary value, preventing overlapping or cramped labels.
- **Transparent/Dark** themes also apply to every account row and the Sync footer, not only the outer card.
- The transparent widget background is lighter to reveal more of the wallpaper.
- Floating Sync feedback shows account, timer counts, and result in a small readable card; literal `\\n` is fixed at both the source and display.
- Existing widgets and account data remain unchanged after installing the update.

### Verification
- APK is compiled and signed by GitHub Actions. Samsung One UI launcher sizing and theme appearance still need on-device confirmation.


## 7.3.0-beta1 — 4 November 2026

### New
- Widget Studio: configure each multi-account widget independently. Choose account selection and sort order, move accounts up/down, change timer text size, show or hide laboratory and pets, and select the classic, dark or translucent background.
- App update checking: see installed and latest published build numbers, release notes and a direct APK link when a public HTTPS update feed is configured. The source repository may stay private.

### Improved
- Smart Sync: copied Clash export JSON and shared/file imports now use the same player-tag-based parser. Incomplete/invalid category snapshots are rejected before existing imported timers are replaced.
- Sync feedback displays counts of builder, laboratory and pet timers.
- Screenshot scanner checks for an upgrade menu first and asks for confirmation when it cannot recognize one.

### Important limitations
- The public update feed and APK download location have not yet been deployed. They require one-time configuration outside the private source repository, documented in `docs/UPDATE_FEED.md`.
- Screenshot OCR is an assisted single-upgrade scan with user review; it is not unattended screen capture and does not scan every upgrade row.
- Widget rendering is built in GitHub Actions; visual inspection on Samsung One UI is still required.

### Safety
- Player tags are the stable account keys. Other accounts and manual timers are not overwritten by a partial Sync.
- Android installation prompts remain mandatory for APK updates.
