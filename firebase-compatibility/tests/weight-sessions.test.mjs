import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { after, before, test } from 'node:test';
import { assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';

let env;
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-sportili-compat', database: {
    host: '127.0.0.1', port: 19000,
    rules: await readFile(new URL('../fixtures/published.database.rules.json', import.meta.url), 'utf8')
  } });
});
after(async () => { await env?.cleanup(); });

test('additive multi-set format survives installed-client edits and cleanup, without a rules deployment', async () => {
  const db = env.unauthenticatedContext().database();
  const user = db.ref('users/local-weight-session-test');
  const old = { noteUtente: 'keep', weightLogs: { old: { timestamp: 1700000000000, weight: 20 } } };
  await user.set({ nome: 'Local', exerciseData: { panca: old } });
  const path = user.child('weightSessions/panca/session-a');
  const payload = { schemaVersion: 1, timestamp: 1790000000000, sets: {
    set_00: { reps: 12, weight: 30 }, set_01: { reps: 10, weight: 35.5 },
    set_02: { reps: 8, weight: 40 }, set_03: { reps: 6, weight: 45 }
  } };
  await assertSucceeds(path.set(payload));
  assert.deepEqual((await user.child('exerciseData/panca').once('value')).val(), old);
  // Legacy clients replace entries; old iOS also removes the exerciseData node when empty.
  await user.child('exerciseData/panca/weightLogs/old').set({ timestamp: 1700000000000, weight: 25 });
  await user.child('exerciseData/panca').remove();
  await user.update({ nome: 'Updated', cognome: 'Local' });
  await user.child('scheda').set({ durata: 4, dataInizio: '2026-09-01T12:00:00+0200' });
  assert.deepEqual((await path.once('value')).val(), payload);
  const edited = { ...payload, sets: { ...payload.sets, set_00: { reps: 12, weight: 32.5 } } };
  await assertSucceeds(path.set(edited));
  await assertSucceeds(path.set(edited));
  assert.equal((await user.child('weightSessions/panca').once('value')).numChildren(), 1);
  assert.equal((await path.child('timestamp').once('value')).val(), payload.timestamp);
  await assertSucceeds(path.remove());
  assert.equal((await path.once('value')).val(), null);
  await user.remove();
});
