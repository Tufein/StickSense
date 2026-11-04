// Pure rekenlogica voor StickSense (geen DOM, goed testbaar).

/** Lengte van een stickvector. */
export const magnitude = (x, y) => Math.hypot(x, y);

/**
 * Radiale deadzone met herschaling: binnen `dz` is de uitvoer 0,
 * daarbuiten loopt de waarde soepel van 0 naar 1.
 */
export function applyDeadzone(x, y, dz) {
  const m = magnitude(x, y);
  if (m <= dz || m === 0) return { x: 0, y: 0 };
  const scale = Math.min(1, (m - dz) / (1 - dz)) / m;
  return { x: x * scale, y: y * scale };
}

/**
 * Schat de benodigde deadzone uit metingen in rust.
 * Neemt het 99e percentiel van de uitslag plus een kleine marge.
 */
export function suggestDeadzone(samples, margin = 0.01) {
  if (!samples.length) return 0;
  const mags = samples.map((s) => magnitude(s.x, s.y)).sort((a, b) => a - b);
  const idx = Math.min(mags.length - 1, Math.ceil(mags.length * 0.99) - 1);
  return Math.min(0.5, +(mags[idx] + margin).toFixed(3));
}

/** Gemiddelde offset (drift) van een reeks rustmetingen. */
export function meanOffset(samples) {
  if (!samples.length) return { x: 0, y: 0 };
  const n = samples.length;
  return {
    x: samples.reduce((a, s) => a + s.x, 0) / n,
    y: samples.reduce((a, s) => a + s.y, 0) / n,
  };
}

/**
 * Cirkelvormigheid uit een rondje langs de rand.
 * Retourneert 0..100 (100 = perfecte cirkel) en de gemiddelde straal.
 * Alleen punten met straal > minRadius tellen mee.
 */
export function circularity(samples, minRadius = 0.5) {
  const radii = samples.map((s) => magnitude(s.x, s.y)).filter((r) => r > minRadius);
  if (radii.length < 8) return { score: null, meanRadius: null, count: radii.length };
  const mean = radii.reduce((a, b) => a + b, 0) / radii.length;
  const variance = radii.reduce((a, r) => a + (r - mean) ** 2, 0) / radii.length;
  const cv = Math.sqrt(variance) / mean;
  return { score: Math.max(0, Math.round((1 - cv * 4) * 100)), meanRadius: mean, count: radii.length };
}

/** Hoek-dekking: in hoeveel van `buckets` sectoren is de rand bereikt? 0..1. */
export function angularCoverage(samples, buckets = 36, minRadius = 0.8) {
  const hit = new Set();
  for (const s of samples) {
    if (magnitude(s.x, s.y) < minRadius) continue;
    const a = (Math.atan2(s.y, s.x) + Math.PI) / (2 * Math.PI);
    hit.add(Math.min(buckets - 1, Math.floor(a * buckets)));
  }
  return hit.size / buckets;
}

/** Oordeel over drift op basis van gemiddelde offset en ruis. */
export function driftVerdict(samples) {
  if (samples.length < 30) return { level: 'onbekend', text: 'Te weinig metingen. Laat de sticks los en meet 3 seconden.' };
  const off = meanOffset(samples);
  const worst = Math.max(...samples.map((s) => magnitude(s.x, s.y)));
  const o = magnitude(off.x, off.y);
  if (o < 0.02 && worst < 0.05) return { level: 'goed', text: 'Geen merkbare drift. Je stick is in orde.' };
  if (o < 0.08 && worst < 0.15) return { level: 'matig', text: 'Lichte drift. Een deadzone van ongeveer ' + suggestDeadzone(samples) + ' lost dit op.' };
  return { level: 'slecht', text: 'Duidelijke drift. Reinigen, kalibreren of vervangen van de stick is aan te raden.' };
}

/** Polling-rate (Hz) uit tijdstempels in ms. */
export function pollingRate(timestamps) {
  if (timestamps.length < 2) return 0;
  const span = timestamps[timestamps.length - 1] - timestamps[0];
  return span > 0 ? Math.round(((timestamps.length - 1) / span) * 1000) : 0;
}

/** Exporteert een meting als CSV-tekst. */
export function toCSV(rows) {
  const head = 't_ms,lx,ly,rx,ry';
  return [head, ...rows.map((r) => [r.t, r.lx, r.ly, r.rx, r.ry].map((v) => +Number(v).toFixed(4)).join(','))].join('\n');
}
