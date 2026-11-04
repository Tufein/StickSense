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

## Beperkingen
Een webapp mag op Android niet zomaar in de mappen van andere apps. Je kiest bestanden of een hele map zelf, en zet bestanden zelf terug in de emulator. Een native Android-app met maptoegang (SAF/Shizuku) en automatische sync is de logische volgende stap, zie `docs/app-ideeen.md`.

## Structuur
- `src/saves.js`: herkenning, manifest en vergelijking van saves
- `src/profiles.js`: profielen
- `src/zip.js`: zip lezen/schrijven met CRC-controle
- `src/store.js`: opslag in IndexedDB
- `src/app.js`: interface
- `docs/`: onderzoek en app-ideeën

## Licentie
MIT
