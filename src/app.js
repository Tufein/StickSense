import { createZip, readZip } from './zip.js';
import { classify, gameName, buildManifest, diffManifests, groupByGame, formatSize } from './saves.js';
import { DEVICES, EMULATORS, makeProfile, upsertProfile, removeProfile, parseProfiles } from './profiles.js';
import * as store from './store.js';

const $ = (s) => document.querySelector(s);
const enc = new TextEncoder();
const dec = new TextDecoder();
let files = [];     // {name, data, kind, label, game, size, addedAt}
let profiles = [];
let pending = null; // geselecteerde back-up voor terugzetten

function h(tag, props = {}, ...kids) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (k === 'class') el.className = v;
    else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else el.setAttribute(k, v);
  }
  for (const kid of kids.flat()) if (kid != null) el.append(kid);
  return el;
}

function toast(msg) {
  const t = $('#toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toast.t);
  toast.t = setTimeout(() => t.classList.remove('show'), 3500);
}

function download(name, bytes, type = 'application/octet-stream') {
  const url = URL.createObjectURL(new Blob([bytes], { type }));
  const a = h('a', { href: url, download: name });
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

const meta = (name, data) => {
  const c = classify(name);
  return { name, data, kind: c ? c.kind : 'overig', label: c ? c.label : 'Bestand', size: data.length };
};

async function load() {
  files = (await store.getAll('files')).sort((a, b) => a.name.localeCompare(b.name));
  profiles = (await store.getAll('profiles')).sort((a, b) => a.name.localeCompare(b.name));
  render();
}

async function addFiles(fileList) {
  let n = 0;
  for (const f of fileList) {
    const name = f.webkitRelativePath || f.name;
    const data = new Uint8Array(await f.arrayBuffer());
    await store.put('files', { ...meta(name, data), addedAt: Date.now() });
    n++;
  }
  toast(`${n} bestand${n === 1 ? '' : 'en'} toegevoegd`);
  await load();
}

// ---------- Saves ----------
function renderSaves() {
  const q = $('#search').value.trim().toLowerCase();
  const kind = $('#kind').value;
  const shown = files.filter((f) => (!kind || f.kind === kind) && (!q || f.name.toLowerCase().includes(q)));
  const totalSize = files.reduce((a, f) => a + f.size, 0);
  $('#stats').textContent = files.length ? `${files.length} bestanden, ${formatSize(totalSize)}. ${shown.length} getoond.` : '';
  const list = $('#list');
  list.replaceChildren();
  if (!files.length) { list.append(h('div', { class: 'empty' }, 'Nog niets in je bibliotheek. Voeg je eerste saves toe hierboven.')); return; }
  if (!shown.length) { list.append(h('div', { class: 'empty' }, 'Geen resultaten.')); return; }
  const entries = shown.map((f) => ({ ...f, game: gameName(f.name.split('/').pop()) }));
  for (const g of groupByGame(entries)) {
    const card = h('div', { class: 'game' }, h('h3', {}, g.game));
    for (const e of g.items) {
      card.append(h('div', { class: 'file' },
        h('span', { class: 'name' }, e.name),
        h('span', { class: 'tag' }, e.label),
        h('span', { class: 'muted' }, formatSize(e.size)),
        h('button', { class: 'btn ghost small', onclick: () => download(e.name.split('/').pop(), files.find((f) => f.name === e.name).data) }, 'Download'),
        h('button', { class: 'btn ghost small', onclick: async () => { await store.del('files', e.name); toast('Verwijderd'); load(); } }, 'Verwijder')));
    }
    list.append(card);
  }
}

// ---------- Profielen ----------
function renderProfiles() {
  const cfgs = files.filter((f) => f.kind === 'config');
  const box = $('#pfiles');
  box.replaceChildren();
  if (!cfgs.length) box.append('Importeer eerst instellingsbestanden (.cfg, .ini, .json) op het tabblad Saves.');
  for (const f of cfgs) box.append(h('label', {}, h('input', { type: 'checkbox', name: 'file', value: f.name }), f.name));
  const list = $('#plist');
  list.replaceChildren();
  if (!profiles.length) { list.append(h('div', { class: 'empty' }, 'Nog geen profielen.')); return; }
  const label = (id) => DEVICES.find((d) => d.id === id)?.label || id;
  for (const p of profiles) {
    list.append(h('div', { class: 'game' },
      h('h3', {}, p.name),
      h('div', { class: 'muted' }, `${label(p.device)} · ${p.emulator} · ${p.files.length} bestand${p.files.length === 1 ? '' : 'en'}`),
      p.notes ? h('p', {}, p.notes) : null,
      h('button', { class: 'btn small', onclick: () => applyProfile(p) }, 'Download bundel'),
      h('button', { class: 'btn ghost small', onclick: async () => { await store.del('profiles', p.id); load(); } }, 'Verwijder')));
  }
}

function applyProfile(p) {
  const have = new Map(files.map((f) => [f.name, f]));
  const missing = p.files.filter((n) => !have.has(n));
  const parts = p.files.filter((n) => have.has(n)).map((n) => ({ name: n, data: have.get(n).data }));
  parts.push({ name: 'LEESMIJ.txt', data: enc.encode(`Profiel: ${p.name}\nEmulator: ${p.emulator}\nPak de bestanden uit en zet ze in de instellingenmap van de emulator.\n${missing.length ? 'Ontbrekend: ' + missing.join(', ') : ''}\n`) });
  download(`${p.id}.zip`, createZip(parts), 'application/zip');
  toast(missing.length ? `Bundel gemaakt, ${missing.length} bestand(en) ontbreken` : 'Bundel gedownload');
}

// ---------- Back-up ----------
async function exportAll() {
  const device = $('#device').value.trim() || 'onbekend';
  const manifest = await buildManifest(files.map((f) => ({ name: 'bestanden/' + f.name, data: f.data })), { device });
  const parts = [
    { name: 'manifest.json', data: enc.encode(JSON.stringify(manifest, null, 2)) },
    { name: 'profielen.json', data: enc.encode(JSON.stringify({ profiles }, null, 2)) },
    ...files.map((f) => ({ name: 'bestanden/' + f.name, data: f.data })),
  ];
  const stamp = new Date().toISOString().slice(0, 10);
  download(`sticksense-backup-${stamp}.zip`, createZip(parts), 'application/zip');
  toast('Back-up gedownload');
}

async function pickBackup(file) {
  try {
    const entries = readZip(new Uint8Array(await file.arrayBuffer()));
    const mf = entries.find((e) => e.name === 'manifest.json');
    if (!mf) throw new Error('Geen StickSense-back-up (manifest ontbreekt)');
    const backup = JSON.parse(dec.decode(mf.data));
    const current = await buildManifest(files.map((f) => ({ name: 'bestanden/' + f.name, data: f.data })));
    const pj = entries.find((e) => e.name === 'profielen.json');
    pending = { entries, diff: diffManifests(backup, current).filter((d) => d.status !== 'alleen-lokaal'), profiles: pj ? parseProfiles(dec.decode(pj.data)) : [] };
    renderDiff(backup);
  } catch (err) {
    toast('Fout: ' + err.message);
  }
}

function renderDiff(backup) {
  const box = $('#diff');
  box.replaceChildren();
  const { diff } = pending;
  box.append(h('p', { class: 'muted' }, `Back-up van ${backup.device}, ${new Date(backup.createdAt).toLocaleString('nl-NL')}. ${diff.length} bestanden, ${pending.profiles.length} profielen.`));
  for (const d of diff) {
    const cb = h('input', { type: 'checkbox', 'data-path': d.path });
    if (d.status === 'nieuw') cb.checked = true; // gewijzigde saves worden nooit automatisch overschreven
    if (d.status === 'gelijk') cb.disabled = true;
    box.append(h('label', {}, h('span', { class: 'file' }, cb, h('span', { class: 'name' }, d.path.replace(/^bestanden\//, '')), h('span', { class: 'tag ' + d.status }, d.status))));
  }
  box.append(h('button', { class: 'btn', onclick: restore }, 'Geselecteerde terugzetten'));
}

async function restore() {
  const chosen = new Set([...document.querySelectorAll('#diff input[data-path]:checked')].map((c) => c.dataset.path));
  let n = 0;
  for (const e of pending.entries) {
    if (!chosen.has(e.name)) continue;
    const name = e.name.replace(/^bestanden\//, '');
    await store.put('files', { ...meta(name, e.data), addedAt: Date.now() });
    n++;
  }
  let list = profiles;
  for (const p of pending.profiles) list = upsertProfile(list, p);
  for (const p of list) await store.put('profiles', p);
  $('#diff').replaceChildren();
  pending = null;
  toast(`${n} bestand${n === 1 ? '' : 'en'} teruggezet`);
  await load();
}

// ---------- UI ----------
function render() { renderSaves(); renderProfiles(); }

document.querySelectorAll('nav button').forEach((b) => b.addEventListener('click', () => {
  document.querySelectorAll('nav button').forEach((x) => x.setAttribute('aria-selected', x === b));
  for (const s of ['saves', 'profielen', 'backup']) $('#tab-' + s).hidden = s !== b.dataset.tab;
}));

for (const d of DEVICES) $('#pdevice').append(h('option', { value: d.id }, d.label));
for (const e of EMULATORS) $('#pemu').append(h('option', { value: e }, e));

$('#pick').addEventListener('change', (e) => addFiles(e.target.files));
$('#pickdir').addEventListener('change', (e) => addFiles(e.target.files));
$('#search').addEventListener('input', renderSaves);
$('#kind').addEventListener('change', renderSaves);
const drop = $('#drop');
['dragenter', 'dragover'].forEach((ev) => drop.addEventListener(ev, (e) => { e.preventDefault(); drop.classList.add('over'); }));
['dragleave', 'drop'].forEach((ev) => drop.addEventListener(ev, (e) => { e.preventDefault(); drop.classList.remove('over'); }));
drop.addEventListener('drop', (e) => addFiles(e.dataTransfer.files));

$('#pform').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  try {
    const p = makeProfile({ name: f.get('name'), device: f.get('device'), emulator: f.get('emulator'), notes: f.get('notes'), files: f.getAll('file') });
    await store.put('profiles', p);
    e.target.reset();
    toast('Profiel opgeslagen');
    load();
  } catch (err) { toast(err.message); }
});
$('#pexport').addEventListener('click', () => download('sticksense-profielen.json', enc.encode(JSON.stringify({ profiles }, null, 2)), 'application/json'));
$('#pimport').addEventListener('change', async (e) => {
  try {
    let list = profiles;
    for (const p of parseProfiles(await e.target.files[0].text())) list = upsertProfile(list, p);
    for (const p of list) await store.put('profiles', p);
    toast('Profielen geïmporteerd');
    load();
  } catch (err) { toast('Fout: ' + err.message); }
  e.target.value = '';
});
$('#export').addEventListener('click', exportAll);
$('#restore').addEventListener('change', (e) => { if (e.target.files[0]) pickBackup(e.target.files[0]); e.target.value = ''; });
$('#wipe').addEventListener('click', async () => {
  if (!confirm('Alle bestanden en profielen in deze browser wissen?')) return;
  await store.clear('files'); await store.clear('profiles');
  toast('Bibliotheek gewist');
  load();
});

if ('serviceWorker' in navigator && location.protocol.startsWith('http')) navigator.serviceWorker.register('sw.js').catch(() => {});
load();
