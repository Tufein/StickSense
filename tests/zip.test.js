import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createZip, readZip, crc32 } from '../src/zip.js';

const bytes = (s) => new TextEncoder().encode(s);

test('crc32 bekende waarde', () => {
  assert.equal(crc32(bytes('123456789')), 0xcbf43926);
});
test('zip roundtrip met unicode-naam en lege data', () => {
  const files = [{ name: 'saves/Pokémon.srm', data: bytes('hallo') }, { name: 'leeg.txt', data: new Uint8Array(0) }];
  const out = readZip(createZip(files));
  assert.equal(out.length, 2);
  assert.equal(out[0].name, 'saves/Pokémon.srm');
  assert.deepEqual([...out[0].data], [...bytes('hallo')]);
});
test('beschadigde data geeft CRC-fout', () => {
  const z = createZip([{ name: 'a.sav', data: bytes('abcdef') }]);
  z[30 + 5] ^= 0xff;
  assert.throws(() => readZip(z), /CRC/);
});
test('geen zip geeft fout', () => {
  assert.throws(() => readZip(bytes('x'.repeat(40))), /zip/);
});
