import { test } from 'node:test';
import assert from 'node:assert/strict';
import { applyDeadzone, suggestDeadzone, meanOffset, circularity, angularCoverage, driftVerdict, pollingRate, toCSV } from '../src/core.js';

const circle = (r, n = 72) => Array.from({ length: n }, (_, i) => ({ x: r * Math.cos((2 * Math.PI * i) / n), y: r * Math.sin((2 * Math.PI * i) / n) }));
const still = (v, n = 60) => Array.from({ length: n }, () => ({ x: v, y: -v }));

test('deadzone nult kleine uitslag', () => {
  assert.deepEqual(applyDeadzone(0.05, 0.05, 0.2), { x: 0, y: 0 });
});
test('deadzone behoudt volle uitslag', () => {
  const o = applyDeadzone(1, 0, 0.2);
  assert.ok(Math.abs(o.x - 1) < 1e-9 && o.y === 0);
});
test('deadzone schaalt continu', () => {
  const o = applyDeadzone(0.6, 0, 0.2);
  assert.ok(Math.abs(o.x - 0.5) < 1e-9);
});
test('suggestDeadzone leeg = 0 en begrensd op 0.5', () => {
  assert.equal(suggestDeadzone([]), 0);
  assert.equal(suggestDeadzone([{ x: 0.9, y: 0 }]), 0.5);
});
test('meanOffset', () => {
  assert.deepEqual(meanOffset([{ x: 0.1, y: 0 }, { x: 0.3, y: 0 }]), { x: 0.2, y: 0 });
});
test('perfecte cirkel scoort 100', () => {
  assert.equal(circularity(circle(1)).score, 100);
});
test('ovaal scoort lager', () => {
  const oval = circle(1).map((p) => ({ x: p.x, y: p.y * 0.8 }));
  assert.ok(circularity(oval).score < 100);
});
test('circularity met te weinig punten geeft null', () => {
  assert.equal(circularity([{ x: 1, y: 0 }]).score, null);
});
test('angularCoverage volledig bij hele cirkel', () => {
  assert.equal(angularCoverage(circle(1, 360)), 1);
  assert.equal(angularCoverage([{ x: 1, y: 0 }]), 1 / 36);
});
test('driftVerdict niveaus', () => {
  assert.equal(driftVerdict(still(0.005)).level, 'goed');
  assert.equal(driftVerdict(still(0.04)).level, 'matig');
  assert.equal(driftVerdict(still(0.3)).level, 'slecht');
  assert.equal(driftVerdict([]).level, 'onbekend');
});
test('pollingRate', () => {
  assert.equal(pollingRate(Array.from({ length: 101 }, (_, i) => i * 8)), 125);
  assert.equal(pollingRate([5]), 0);
});
test('toCSV', () => {
  assert.equal(toCSV([{ t: 0, lx: 0.5, ly: 0, rx: 0, ry: 1 }]), 't_ms,lx,ly,rx,ry\n0,0.5,0,0,1');
});
