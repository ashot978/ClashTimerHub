### NL
- De Logger-onderbouw in de Clash-export (`guardians`, ID 107000008) wordt nu als actieve bouwupgrade geïmporteerd en telt mee bij de bezette bouwers.
- Bij een export met 5 Wizard Towers, 1 Revenge Tower en 1 Logger worden alle 7 actieve bouwtimers herkend; Guardians zonder actieve timer worden genegeerd.
- Een gedeeltelijke export zonder `guardians` kan bestaande actieve Guardian-timers niet stilzwijgend wissen. Overige timers, accounts en widgetindelingen blijven ongewijzigd.

### EN
- Logger upgrades in Clash exports (`guardians`, ID 107000008) now import as active builder upgrades and count toward occupied builders.
- An export with 5 Wizard Towers, 1 Revenge Tower and 1 Logger now recognizes all 7 active builder timers; Guardians without an active timer are ignored.
- Partial exports missing `guardians` cannot silently erase previously active Guardian timers. Other timers, accounts and widget layouts are unchanged.
