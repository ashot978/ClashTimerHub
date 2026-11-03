# Clash Timer Hub — Android (prototype)

Een Android-widget voor zeven Clash of Clans-accounts, met per account de eerstvolgende timer voor bouwers, laboratorium en pets.

## Status
- Android-broncode is geüpload als gecomprimeerd bronarchief in `source/chunk01.b64` t/m `chunk06.b64`.
- Het workflowbestand `.github/workflows/build-apk.yml` reconstrueert de volledige Android-projectstructuur, controleert de SHA-256-checksum en bouwt een debug-APK.
- Een APK is pas beschikbaar nadat **Build Android test APK** groen is.
- De scanner en timerwidget zijn nog niet op een echte Samsung getest.

## APK downloaden (als de workflow geslaagd is)
Open het tabblad [Actions](https://github.com/ashot978/ClashTimerHub/actions), selecteer **Build Android test APK** en kies de geslaagde run. Download het artifact **ClashTimerHub-debug-apk** (ZIP), pak het uit op je telefoon en open `app-debug.apk` om te installeren.

## Broncode lokaal uitpakken
```bash
cat source/chunk*.b64 | base64 --decode > clashtimerhub-sources.tar.xz
tar -xJf clashtimerhub-sources.tar.xz
```
Het archief bevat de volledige Java-, Android-resource- en Gradle-projectbestanden. Deze archiefmethode wordt alleen gebruikt omdat de aangesloten GitHub-uploader geen directe binaire ZIP-upload ondersteunt.

## Werking
Selecteer een van zeven accounts en Bouwers/Laboratorium/Pets. Geef toestemming voor een korte Android-schermdeelsessie, bekijk de timer in het spel en bevestig de gevonden OCR-timer in de app. Er worden geen spelhandelingen uitgevoerd en geen Supercell-inloggegevens gevraagd.

**Let op:** dit is een experimentele versie. OCR kan timers missen of verwarren; de app vraagt daarom om bevestiging voor het opslaan.
