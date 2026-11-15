# Pull request checklist

- [ ] `npm test` slaagt (12 tests, `node --test`, Node 20+, geen dependencies)
- [ ] `npm start` en de getroffen flow handmatig in de browser nagekeken; bij een PWA-wijziging ook
      één keer **offline** terugzetten
- [ ] `sw.js`: `CACHE`-naam verhoogd en nieuwe bestanden toegevoegd aan `FILES`
- [ ] Formaatwijziging in zip of manifest? Tests in **beide** richtingen (`tests/*.js` én
      `./gradlew :core:test` in `android/`), want oudere back-ups moeten leesbaar blijven
- [ ] Terugzetten maakt nog steeds zichtbaar wat nieuw, gewijzigd of gelijk is; niets wordt
      stilzwijgend overschreven
- [ ] Er is geen `fetch` in `src/` nodig geworden — zo ja, leg uit waarom en werk
      [SECURITY.md](../SECURITY.md) bij
- [ ] Interface-teksten blijven Nederlands, in `index.html`/`src/app.js` en in de native app
