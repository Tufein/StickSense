import { test } from 'node:test';
import assert from 'node:assert/strict';
import { classify, gameName, buildManifest, diffManifests, groupByGame, formatSize } from '../src/saves.js';
import { makeProfile, upsertProfile, removeProfile, parseProfiles, missingFiles } from '../src/profiles.js';

const b = (s) => new TextEncoder().encode(s);

test('classify', () => {
  assert.equal(classify('Zelda.srm').kind, 'save');
  assert.equal(classify('Zelda.state3').kind, 'state');
  assert.equal(classify('Zelda.state.auto').kind, 'state');
  assert.equal(classify('retroarch.cfg').kind, 'config');
  assert.equal(classify('rom.zip'), null);
});
test('gameName', () => {
  assert.equal(gameName('Zelda.srm'), 'Zelda');
  assert.equal(gameName('Zelda.state3'), 'Zelda');
  assert.equal(gameName('Zelda.state.auto'), 'Zelda');
});
test('manifest en diff', async () => {
  const a = await buildManifest([{ name: 'A.srm', data: b('1') }, { name: 'B.srm', data: b('2') }]);
  const c = await buildManifest([{ name: 'A.srm', data: b('1') }, { name: 'B.srm', data: b('3') }, { name: 'C.srm', data: b('4') }]);
  const d = Object.fromEntries(diffManifests(a, c).map((x) => [x.path, x.status]));
  assert.deepEqual(d, { 'A.srm': 'gelijk', 'B.srm': 'gewijzigd', 'C.srm': 'alleen-lokaal' });
  assert.equal(a.entries[0].sha256.length, 64);
});
test('groupByGame sorteert', async () => {
  const m = await buildManifest([{ name: 'B.srm', data: b('1') }, { name: 'A.srm', data: b('1') }, { name: 'A.state1', data: b('1') }]);
  const g = groupByGame(m.entries);
  assert.deepEqual(g.map((x) => [x.game, x.items.length]), [['A', 2], ['B', 1]]);
});
test('formatSize', () => {
  assert.equal(formatSize(512), '512 B');
  assert.equal(formatSize(2048), '2.0 KB');
});
test('profiel valideert en begrenst', () => {
  assert.throws(() => makeProfile({ name: ' ' }));
  const p = makeProfile({ name: 'Mijn Odin!', files: ['a.cfg', 'a.cfg', 'b.cfg'] });
  assert.equal(p.id, 'mijn-odin');
  assert.deepEqual(p.files, ['a.cfg', 'b.cfg']);
});
test('upsert, remove en import', () => {
  let l = upsertProfile([], makeProfile({ name: 'A' }));
  l = upsertProfile(l, makeProfile({ name: 'A', emulator: 'Dolphin' }));
  assert.equal(l.length, 1);
  assert.equal(l[0].emulator, 'Dolphin');
  assert.equal(removeProfile(l, 'a').length, 0);
  assert.equal(parseProfiles(JSON.stringify({ profiles: [{ name: 'X' }] }))[0].id, 'x');
  assert.throws(() => parseProfiles('nope'), /JSON/);
});
test('missingFiles', async () => {
  const m = await buildManifest([{ name: 'a.cfg', data: b('1') }]);
  assert.deepEqual(missingFiles(makeProfile({ name: 'P', files: ['a.cfg', 'b.cfg'] }), m), ['b.cfg']);
});
