# Project Killshot Wifi

**Goal:** make Telenet either deliver the "onbeperkt" (unlimited) internet it advertises, or stop calling it unlimited, using lawful consumer routes, hard evidence, and smart network habits.

> This is a consumer-rights plan, not legal advice. Everything here is legal: complaints, measurements on your own network, and contract choices. No tampering with Telenet's network, no modem hacks, no ToS-breaking circumvention. Those would hand Telenet a reason to end your contract and would undo your case.

---

## 1. Know the playing field

| Topic | What applies | Source |
|---|---|---|
| BIPT "unlimited" guidelines (Feb 2022) | Operators may only say "unlimited" if the volume lets the vast majority of customers surf without speed limits. Benchmark fair-use (FUP) threshold: **3 TB/month fixed**, **300 GB/month mobile**. | [BIPT: More clarity on unlimited surfing](https://bipt.be/consumers/publication/more-clarity-on-unlimited-surfing), [BIPT guidelines](https://bipt.be/operators/publication/bipt-communication-regarding-guidelines-for-the-provision-of-unlimited-internet) |
| Telenet practice | Telenet fixed "unlimited" has a FUP (reported around 3 TB); above it the speed is reduced until the next billing period. Mobile ONE "unlimited" is reported at 300 GB FUP. **Check your own contract's Bijzondere Voorwaarden for the exact numbers.** | [Test-Aankoop](https://www.test-aankoop.be/hightech/telecom/nieuws/onbeperkt-internet-moet-zonder-vermindering-van-snelheid), [Telecompaper](https://www.telecompaper.com/news/telenet-blijft-mobiel-internet-bij-one-bundel-onbeperkt-noemen-ondanks-snelheidsverlaging--1396227), [Telenet infofiche](https://www2.telenet.be/content/dam/www-telenet-touch/nl/klantenservice/infofiches/infofiche-internet.pdf) |
| EU Open Internet Regulation 2015/2120 | Art. 4(1) requires the contract to state data limits and their effect on speed. Art. 4(4): a significant, continuous or regularly recurring gap between actual and contracted speed counts as non-conformity and opens contractual remedies. | [EUR-Lex 2015/2120](https://eur-lex.europa.eu/eli/reg/2015/2120/oj) |
| Belgian Code of Economic Law, Book VI | Misleading commercial practices (unfair practices toward consumers) are prohibited. This is the hook for advertising complaints. | FOD Economie |
| Consumer pressure | Test-Aankoop has argued that home internet sold as "onbeperkt" should have **no** FUP. VRT's De Inspecteur pushed Telenet to adjust its website in 2021. | [Test-Aankoop](https://www.test-aankoop.be/hightech/telecom/nieuws/onbeperkt-internet-moet-zonder-vermindering-van-snelheid), [VRT NWS](https://www.vrt.be/vrtnws/nl/2021/09/07/telenet-past-website-aan-na-klacht-bij-de-inspecteur-over-onbepe/) |

**Bottom line:** if Telenet throttles you **below 3 TB** on fixed (or 300 GB on mobile), or the throttle is not clearly in your contract, you have a strong case. If you are throttled **above** that threshold, your fight is mostly about the *word* "unlimited" in the advertising (route B below) plus switching to a plan without a FUP.

---

## 2. Phase 0: Gather evidence (start today, run 1–2 billing cycles)

You win these fights with logs, not with anger.

1. **Save the advertising.** Screenshot and PDF-print every Telenet page, folder, TV/YouTube ad, and email that says "onbeperkt", with date and URL. Use the Wayback Machine (web.archive.org → "Save Page Now") so the evidence is timestamped by a third party.
2. **Save your contract.** Download your current Algemene + Bijzondere Voorwaarden, the tariff plan fiche, and your order confirmation from My Telenet. Note the exact FUP text, threshold, and the throttled speed.
3. **Log your usage.** Screenshot My Telenet's volume meter daily near the end of the cycle. Log your own counters too (see `tools/`).
4. **Log your speed.** Run `tools/speedlog.sh` from a wired machine every 30 minutes (cron). It writes a CSV you can graph.
5. **Log every contact.** Every call, chat and email goes in `evidence/evidence-log.csv`: date, channel, agent name, ticket number, what was promised.

See [`evidence/README.md`](evidence/README.md) for how to keep this clean and credible.

---

## 3. Escalation ladder

Climb one step at a time. Each step needs the previous one documented.

### Route A: your own throttling (individual dispute)

| Step | Who | How | Wait |
|---|---|---|---|
| A1 | **Telenet klantendienst** | Written complaint via My Telenet or email, use [`templates/01-klacht-telenet.md`](templates/01-klacht-telenet.md). Ask for a ticket number. | 10 working days |
| A2 | **Telenet formal complaint / ingebrekestelling** | Registered letter (aangetekend) with [`templates/02-ingebrekestelling.md`](templates/02-ingebrekestelling.md). Demand: lift throttle, credit the throttled period, or free switch/termination. | 15 days |
| A3 | **Ombudsdienst voor Telecommunicatie** | Free. File online at [ombudsman.be](https://www.ombudsman.be). Attach evidence log + speed CSV + Telenet's answers. Use [`templates/03-ombudsdienst.md`](templates/03-ombudsdienst.md). Telenet must respond; recommendations carry weight. | Typically weeks |
| A4 | **Test-Aankoop** | Post a public complaint on the Test-Aankoop "Klagen" platform; members get legal help. Public complaints put visible pressure on the brand. | — |
| A5 | **Vredegerecht** (justice of the peace) | Last resort for refund/damages. Small amounts, no lawyer required. Your logs are your case. | Months |

### Route B: the "unlimited" advertising itself (market-level)

| Step | Who | How |
|---|---|---|
| B1 | **FOD Economie – ConsumentConnect / Meldpunt** | Report misleading advertising at [meldpunt.belgie.be](https://meldpunt.belgie.be). Their Inspectie can investigate and fine. Use [`templates/04-meldpunt-reclame.md`](templates/04-meldpunt-reclame.md). |
| B2 | **JEP (Jury voor Ethische Praktijken inzake reclame)** | Free complaint form at [jep.be](https://www.jep.be) against a specific ad (TV, online, print). Attach the screenshot/URL and date. |
| B3 | **BIPT** | BIPT does not settle individual disputes, but does use consumer signals to check compliance with its "unlimited" guidelines and the Open Internet Regulation. Send a short signal with your data via [bipt.be](https://www.bipt.be). |
| B4 | **Media & consumer orgs** | VRT De Inspecteur, Test-Aankoop, tech press (Tweakers BE, DataNews). Bring data: "advertised unlimited, throttled at X TB, here's the graph." |

Route A and B can run in parallel. Route B is where the *marketing* changes; Route A is where *your* line gets fixed or compensated.

---

## 4. Technical playbook: prove it, and stay under it

All of this runs on **your own** network. Nothing touches Telenet equipment beyond normal use.

### Prove the throttle
- **Wired baseline:** measure from a machine on Ethernet directly behind the modem, Wi-Fi off, other devices idle. Wi-Fi results are worthless as evidence.
- **Scheduled tests:** `tools/speedlog.sh` (Ookla `speedtest` CLI or `speedtest-cli`, plus optional `iperf3` to a public server). Run via cron every 30 min for the full cycle.
- **Counter your counter:** log WAN bytes from your own router (OPNsense/pfSense traffic graphs, `vnstat` on a Linux router, or SNMP into Grafana). If Telenet's meter and yours disagree, that's evidence too.
- **The money shot:** one graph with *cumulative GB used* and *download speed* on the same timeline. The step down at the threshold is your case in one picture.

### Avoid hitting the cap (legal and in your control)
- **Find the hogs:** per-device stats on your router. Usual suspects: game launchers auto-updating (Steam, Battle.net, consoles), cloud backups, 4K streaming, security cameras uploading.
- **Schedule big downloads** early in the billing cycle, cap launcher download bandwidth, turn off auto-update for games you're not playing.
- **Cache locally:** a LAN game cache (e.g. LANCache) means each game update comes down once for all PCs/consoles. Great for a retro/gaming household.
- **Stream smarter:** 1080p instead of 4K where you can't tell the difference saves ~50–70%.
- **Upload counts too** on most plans: check backup and camera upload settings.

### Escape the cap
- Ask Telenet in writing for a plan **without** FUP, or a business (Telenet Business) line, and get the conditions in writing.
- Compare alternatives that advertise "zonder fair use": check current offers on [BIPT's price comparison tool](https://www.bestetarief.be) and operators like Proximus, Orange, Mobile Vikings, Digi. Throttling outside your contract terms is usually grounds to leave without penalty; ask the Ombudsdienst to confirm.

---

## 5. Timeline

| Week | Action |
|---|---|
| 0 | Save ads + contract, start speed and usage logging, open evidence log |
| 0–4 | Run one full billing cycle of measurements |
| 4 | A1 complaint to Telenet + B1 Meldpunt report + B2 JEP complaint |
| 6 | No fix → A2 registered letter |
| 8 | No fix → A3 Ombudsdienst, B3 BIPT signal, A4 Test-Aankoop public complaint |
| 12+ | Ombudsdienst outcome → accept, switch provider, or A5 vredegerecht |

---

## Repo layout

```
README.md                  this plan
evidence/README.md         how to collect evidence that holds up
evidence/evidence-log.csv  contact + incident log template
templates/                 complaint letters (Dutch), ready to fill in
tools/speedlog.sh          scheduled speed logger -> CSV
tools/plot.py              graph usage vs. speed from the CSV
```

## Sources
- [BIPT: More clarity on unlimited surfing (2022)](https://bipt.be/consumers/publication/more-clarity-on-unlimited-surfing)
- [BIPT communication: guidelines for unlimited internet](https://bipt.be/operators/publication/bipt-communication-regarding-guidelines-for-the-provision-of-unlimited-internet)
- [Test-Aankoop: onbeperkt internet moet zonder vermindering van snelheid](https://www.test-aankoop.be/hightech/telecom/nieuws/onbeperkt-internet-moet-zonder-vermindering-van-snelheid)
- [Telecompaper: Telenet blijft mobiel internet 'onbeperkt' noemen](https://www.telecompaper.com/news/telenet-blijft-mobiel-internet-bij-one-bundel-onbeperkt-noemen-ondanks-snelheidsverlaging--1396227)
- [VRT NWS: Telenet past website aan na klacht bij De Inspecteur (2021)](https://www.vrt.be/vrtnws/nl/2021/09/07/telenet-past-website-aan-na-klacht-bij-de-inspecteur-over-onbepe/)
- [EU Regulation 2015/2120 (Open Internet)](https://eur-lex.europa.eu/eli/reg/2015/2120/oj)
- [Telenet infofiche internet](https://www2.telenet.be/content/dam/www-telenet-touch/nl/klantenservice/infofiches/infofiche-internet.pdf)

*Figures (3 TB / 300 GB) are from BIPT's 2022 guidelines and secondary reporting; verify against your own current contract before sending any letter.*
