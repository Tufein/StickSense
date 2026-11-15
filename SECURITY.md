# Beveiliging en onderhoud

StickSense bewaart je saves en profielen alleen op je eigen toestel. De webapp doet geen enkel
netwerkverzoek: er is geen account, geen server en geen telemetry (geen `fetch` of
`XMLHttpRequest` in `src/`). Deel bij een bugmelding dus alleen bestandsnamen en
schermafbeeldingen, nooit een back-upzip met spelstanden van derden.

Gebruik GitHub private vulnerability reporting **als de eigenaar die route heeft ingeschakeld**. Er
is geen apart vertrouwelijk contactadres in deze repo. Werkt dat niet, open dan een korte issue
waarin je om een private contactroute vraagt, zonder exploitdetails.

## Wat de app vertrouwt

- **IndexedDB is de enige opslag** (`src/store.js`). De browser kan die data opschonen bij
  opslagdruk; er is nog geen `navigator.storage.persist()`-verzoek. Voor belangrijke saves: maak een
  export-zip en bewaar die buiten de browser.
- **Back-upzip's zijn ongecodeerd** en bevatten wat erin zit: saves, emulatorinstellingen en
  profielnotities. De manifest bevat een SHA-256 per bestand, zodat terugzetten zichtbaar maakt wat
  er verandert is; gewijzigde saves worden nooit automatisch overschreven.
- **De development-server** (`scripts/serve.js`) draait op `localhost` en is alleen voor lokaal
  gebruik; hij heeft geen authenticatie en hoort niet openbaar bereikbaar te zijn.
- **De native app** gebruikt alleen het Storage Access Framework: geen `INTERNET`-machtiging en geen
  opslagrechten buiten de mappen die jij kiest.

## Onderhoud

- Draai `npm test` (de JS-logica) en `./gradlew :core:test` (de roundtrip-tests in `android/core`)
  vóór elke release; de PWA-cache-versie in `sw.js` moet bij elke publicatie omhoog.
- Web en Android moeten elkaars zip- en manifestformaat blijven lezen: test na een formaatwijziging
  beide richtingen en leg die controle in de release-notities vast.
- Automatische sync (WebDAV/Syncthing) staat op de roadmap; pas dan komt authenticatie, een
  externe-opslagroute en het beleid voor sleutels aan de orde en hoort dit document mee te werken.

Deze tekst beschrijft het huidige ontwerp, geen audit. Er wordt geen reactietijd toegezegd.
