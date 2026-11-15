# Bijdragen aan StickSense

StickSense beheert saves en profielen voor Android-handhelds, volledig in de browser. De web-app is
het uitgangspunt; `android/` is dezelfde logica in Kotlin, zonder account of server.

## Oriëntatie

- `src/` is de web-app: `saves.js` (herkenning, manifest, vergelijking), `profiles.js`, `zip.js`
  (lezen/schrijven met CRC-controle), `store.js` (IndexedDB), `app.js` (interface).
- `android/core/` bevat de pure Kotlin-logica (zip, manifest, profielen); `android/app/` de UI en het
  Storage Access Framework.
- [docs/onderzoek-ayn-retroid.md](docs/onderzoek-ayn-retroid.md) en
  [docs/app-ideeen.md](docs/app-ideeen.md) houden het apparaatonderzoek en de open ideeën bij.

## Testen en draaien

```sh
npm test          # node --test, 12 tests, geen dependencies (Node 20+)
npm start         # http://localhost:8080
cd android && ./gradlew :core:test :app:assembleDebug
```

Beide CI-workflows draaien precies dit. De tests dekken nu de pure logica; raak je iets aan van
`store.js`, `app.js` of `sw.js`, test dat dan in de browser (Incognito-installatie en offline
terugzetten tellen als echte checks) en zeg in je pull request wat je hebt gedaan.

## Gebruikskosten

- **De web-app raakt nooit het net**: er is geen `fetch` in `src/`. Een nieuw idee dat sync of een
  externe dienst vraagt, hoort eerst in [docs/app-ideeen.md](docs/app-ideeen.md) te staan.
- **Web en Android moeten elkaars formaat blijven lezen.** Een wijziging in het zip- of
  manifestformaat krijgt tests in beide richtingen (`tests/*.js` én `android/core`), anders breekt
  elke back-up uit een oudere versie.
- **`sw.js`: verhoog `CACHE` bij elke publicatie**, anders blijft een geïnstalleerde PWA op de oude
  `src/*.js` hangen. Voeg nieuwe bestanden toe aan `FILES`.
- **Terugzetten overschrijft nooit stilletjes.** Het verschil (nieuw/gewijzigd/gelijk) blijft zichtbaar
  en de SHA-256 per bestand in het manifest is het bewijs daarvan.
- **Interface-teksten zijn Nederlands.** In de web-app staan ze in `index.html` en `src/app.js`; de
  native app zet ze nu letterlijk in `MainActivity.kt` (er is nog geen `strings.xml`). Nieuwe teksten
  krijgen dezelfde formulier als hun web-equivalent; wil je ze naar bronnen verplaatsen, doe dat dan
  als aparte pull request.

## Stijl

Geen build-stap en geen npm-afhankelijkheden in de web-app — houd het bij ES-modules. Komt er toch een
dependency, leg uit waarom en voeg `package.json`-lockgedrag toe aan de README.
