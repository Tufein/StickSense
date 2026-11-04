# StickSense

Save- en profielbeheer voor AYN- en Retroid-handhelds (Odin 2, Thor, Retroid Pocket 5, Mini, Flip 2 en andere Android-handhelds). Draait volledig in de browser, installeerbaar als app, zonder account of server. Je bestanden blijven op je eigen toestel.

## Wat kun je ermee?
- **Saves beheren:** importeer saves (`.srm`, `.sav`, `.mcr`, `.eep`, ...), savestates en emulatorinstellingen. Alles wordt per spel gegroepeerd, doorzoekbaar en te filteren.
- **Profielen:** leg per toestel en emulator een bundel instellingsbestanden vast met notities, en download die als zip voor een nieuw toestel. Profielen zijn als JSON te exporteren en importeren.
- **Back-up en terugzetten:** één zip met alles (manifest met SHA-256 per bestand). Bij terugzetten zie je eerst wat nieuw, gewijzigd of gelijk is. Gewijzigde saves worden nooit automatisch overschreven.

## Gebruik
```
npm start        # http://localhost:8080
npm test         # 12 tests, Node 20+, geen dependencies
```
Op een handheld: open de site in de browser en kies "Aan startscherm toevoegen".

## Native Android-app (`android/`)
De webapp mag op Android niet in de mappen van andere apps. De native app (Kotlin, Jetpack Compose) gebruikt daarom de Storage Access Framework: je kiest één keer de map van een emulator (bijv. RetroArch `saves`) als **bron**, en de app onthoudt de toegang.

- **Saves:** alle bronnen scannen, per spel groeperen, zoeken.
- **Profielen:** bundel instellingsbestanden per toestel en emulator, met notities. Een profiel waarvan bestanden ontbreken op dit toestel wordt gemarkeerd.
- **Back-up:** zip in een gekozen map, met manifest en SHA-256 per bestand. Savestates en overige bestanden zijn optioneel.
- **Terugzetten:** eerst nieuw/gewijzigd/gelijk bekijken. Alleen nieuwe bestanden staan standaard aangevinkt, gewijzigde saves overschrijf je alleen bewust. Zips met onveilige paden of kapotte hashes worden geweigerd.
- Het zip-formaat is hetzelfde als dat van de webapp.

```
cd android
./gradlew :core:test           # 8 tests voor de kernlogica
./gradlew :app:assembleDebug   # APK in app/build/outputs/apk/debug/
```
Vereist JDK 17+ en de Android SDK (platform 34). De APK is gebouwd, maar nog niet op een echt toestel of emulator getest.

## Beperkingen
Back-ups worden in het geheugen opgebouwd; zeer grote bibliotheken (honderden MB aan savestates) kunnen daarom zwaar zijn. Automatische sync en toegang tot `Android/data` (vanaf Android 11 afgeschermd) staan nog op de planning, zie `docs/app-ideeen.md`.

## Structuur
- `src/saves.js`: herkenning, manifest en vergelijking van saves
- `src/profiles.js`: profielen
- `src/zip.js`: zip lezen/schrijven met CRC-controle
- `src/store.js`: opslag in IndexedDB
- `src/app.js`: interface
- `android/`: native app (`core` = pure Kotlin-logica, `app` = interface)
- `docs/`: onderzoek en app-ideeën

## Licentie
MIT
