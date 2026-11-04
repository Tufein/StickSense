// Herkenning en beheer van savebestanden en emulatorprofielen.

export const SAVE_TYPES = {
  srm: { label: 'RetroArch-save', kind: 'save' },
  sav: { label: 'Save', kind: 'save' },
  mcr: { label: 'PS1-geheugenkaart', kind: 'save' },
  mcd: { label: 'PS1-geheugenkaart', kind: 'save' },
  ps2: { label: 'PS2-geheugenkaart', kind: 'save' },
  eep: { label: 'N64-save', kind: 'save' },
  fla: { label: 'N64-save', kind: 'save' },
  dsv: { label: 'DS-save', kind: 'save' },
  state: { label: 'Savestate', kind: 'state' },
  ini: { label: 'Emulatorinstelling', kind: 'config' },
  cfg: { label: 'Emulatorinstelling', kind: 'config' },
  json: { label: 'Profiel', kind: 'config' },
};

const STATE_RE = /\.state\d*(\.auto)?$/i;

/** Bepaalt type van een bestandsnaam, of null als het niet herkend wordt. */
export function classify(name) {
  if (STATE_RE.test(name)) return { ext: 'state', ...SAVE_TYPES.state };
  const m = /\.([a-z0-9]+)$/i.exec(name);
  if (!m) return null;
  const ext = m[1].toLowerCase();
  return SAVE_TYPES[ext] ? { ext, ...SAVE_TYPES[ext] } : null;
}

/** Spel-naam zonder extensie, voor groeperen per spel. */
export function gameName(fileName) {
  return fileName.replace(/\.(state\d*(\.auto)?|[a-z0-9]{2,4})$/i, '').trim();
}

/** SHA-256 hex van bytes (WebCrypto, aanwezig in browsers en Node 20+). */
export async function sha256(bytes) {
  const d = await crypto.subtle.digest('SHA-256', bytes);
  return [...new Uint8Array(d)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

/** Maakt een manifest voor een lijst bestanden. */
export async function buildManifest(files, { device = 'onbekend', now = new Date() } = {}) {
  const entries = [];
  for (const f of files) {
    const c = classify(f.name);
    entries.push({
      path: f.name,
      kind: c ? c.kind : 'overig',
      label: c ? c.label : 'Bestand',
      game: gameName(f.name.split('/').pop()),
      size: f.data.length,
      sha256: await sha256(f.data),
    });
  }
  return { app: 'StickSense', version: 1, device, createdAt: now.toISOString(), entries };
}

/**
 * Vergelijkt een back-up met de huidige bestanden.
 * Retourneert per pad: nieuw, gewijzigd, gelijk of ontbreekt (alleen in huidige set).
 * Voorkomt dat een nieuwere save stil overschreven wordt.
 */
export function diffManifests(backup, current) {
  const cur = new Map(current.entries.map((e) => [e.path, e]));
  const result = [];
  for (const b of backup.entries) {
    const c = cur.get(b.path);
    if (!c) result.push({ path: b.path, status: 'nieuw', entry: b });
    else if (c.sha256 === b.sha256) result.push({ path: b.path, status: 'gelijk', entry: b });
    else result.push({ path: b.path, status: 'gewijzigd', entry: b, currentSize: c.size });
    cur.delete(b.path);
  }
  for (const [path, entry] of cur) result.push({ path, status: 'alleen-lokaal', entry });
  return result;
}

/** Groepeert manifest-entries per spel. */
export function groupByGame(entries) {
  const map = new Map();
  for (const e of entries) {
    if (!map.has(e.game)) map.set(e.game, []);
    map.get(e.game).push(e);
  }
  return [...map.entries()].map(([game, items]) => ({ game, items })).sort((a, b) => a.game.localeCompare(b.game));
}

export function formatSize(n) {
  if (n < 1024) return n + ' B';
  if (n < 1048576) return (n / 1024).toFixed(1) + ' KB';
  return (n / 1048576).toFixed(1) + ' MB';
}
