// Profielen: een benoemde bundel emulatorbestanden (instellingen, controllerprofielen)
// voor één toestel en emulator. Bestanden zelf zitten in de back-up; het profiel
// bevat de metadata en de lijst met paden.

export const DEVICES = [
  { id: 'ayn-odin2', label: 'AYN Odin 2' },
  { id: 'ayn-odin2-portal', label: 'AYN Odin 2 Portal' },
  { id: 'ayn-thor', label: 'AYN Thor' },
  { id: 'retroid-pocket-5', label: 'Retroid Pocket 5' },
  { id: 'retroid-pocket-mini', label: 'Retroid Pocket Mini' },
  { id: 'retroid-flip-2', label: 'Retroid Pocket Flip 2' },
  { id: 'overig', label: 'Ander toestel' },
];

export const EMULATORS = ['RetroArch', 'DuckStation', 'NetherSX2', 'Dolphin', 'PPSSPP', 'melonDS', 'Azahar', 'Winlator', 'Overig'];

const slug = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'profiel';

/** Normaliseert en valideert een profiel; gooit bij ongeldige invoer. */
export function makeProfile({ name, device = 'overig', emulator = 'Overig', notes = '', files = [] }) {
  const n = String(name || '').trim();
  if (!n) throw new Error('Geef het profiel een naam');
  return {
    id: slug(n),
    name: n,
    device,
    emulator,
    notes: String(notes).slice(0, 500),
    files: [...new Set(files.map(String))],
    updatedAt: new Date().toISOString(),
  };
}

/** Voegt toe of vervangt op id; geeft nieuwe lijst terug. */
export function upsertProfile(list, profile) {
  const i = list.findIndex((p) => p.id === profile.id);
  const next = list.slice();
  if (i >= 0) next[i] = profile; else next.push(profile);
  return next;
}

export const removeProfile = (list, id) => list.filter((p) => p.id !== id);

/** Leest geïmporteerde JSON en geeft alleen geldige profielen terug. */
export function parseProfiles(json) {
  let data;
  try { data = JSON.parse(json); } catch { throw new Error('Geen geldige JSON'); }
  const arr = Array.isArray(data) ? data : data.profiles;
  if (!Array.isArray(arr)) throw new Error('Geen profielen gevonden');
  return arr.map((p) => makeProfile(p));
}

/** Welke bestanden uit een profiel ontbreken in een back-up? */
export function missingFiles(profile, manifest) {
  const have = new Set(manifest.entries.map((e) => e.path));
  return profile.files.filter((f) => !have.has(f));
}
