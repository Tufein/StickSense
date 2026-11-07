# App-ideeën voor AYN- en Retroid-handhelds

Datum: 2026-11-04. Bron: webonderzoek (Reddit was niet bereikbaar, dus geen directe community-peilingen).

## Wat al bestaat (dus niet opnieuw bouwen)
- **Launchers/frontends:** [ES-DE, Daijishō en Beacon](https://heldgames.com/guides/best-emulation-frontends) dekken dit goed. ES-DE is betaald op Android, Daijishō gratis, Beacon minimaal. Markt is bezet.
- **Prestaties/ventilator:** [PULSE](https://newreleases.io/project/github/keiretrogaming/pulse/release/v1.19.1) (no-root tuner met fancontrol, CPU/GPU, fps-limiet) en OdinTools voor AYN.
- **Installeren/updaten:** Obtainium.
- **In-game menu's voor ES-DE:** [HandheldExp](https://github.com/Teppichseite/HandheldExp).

## Gaten die open feature requests laten zien
1. **Saves synchroniseren tussen handhelds en pc.** Open verzoeken vragen om sync van `.srm/.sav` met controle op ROM-hash en core-versie, en om een Android-companion voor RetroArch ([verzoek 1](https://github.com/InukSaiyan/Retro-Game-Clubs/issues/1), [verzoek 2](https://github.com/magoon-labs/gamelord/issues/228)). Pijn: DuckStation en anderen zitten achter Android Scoped Storage, dus handmatig kopiëren lukt vaak niet.
2. **Draagbare setup-profielen.** Controllerprofielen, remaps en per-game instellingen meenemen naar een nieuw toestel ([verzoek](https://github.com/InukSaiyan/Retro-Game-Clubs/issues/2)). Elke emulator doet dit anders.
3. **Stickdrift-tester en deadzone-advies** per toestel. Bestaat als losse tools, maar niet geïntegreerd met profielen.

## Voorstel
| Idee | Waarde | Haalbaarheid | Concurrentie |
|---|---|---|---|
| A. **Save- en profielbeheer** (back-up, sync, herstel, nieuw-toestel-assistent) | Hoog | Matig: Scoped Storage vraagt SAF-toegang of Shizuku | Laag, alleen losse scripts |
| B. Stick-tester met deadzone-advies | Middel | Hoog | Middel |
| C. Nieuwe launcher | Middel | Laag werk, hoge concurrentie | Hoog |

**Aanbeveling:** A als kern, met B als ingebouwde module (de stick-logica staat al in de repo). Eerste versie: Android-app (Kotlin of Flutter) die saves en emulatorconfig detecteert via toegang tot de map, lokale back-ups maakt, en synchroniseert via een eigen map, Syncthing of WebDAV. Geen eigen server nodig.
