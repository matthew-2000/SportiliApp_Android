import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { after, before, beforeEach, test } from 'node:test';
import { assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';

// Fixed loopback endpoints and a demo ID: no production SDK config or credentials.
const projectId = 'demo-sportili-compat';
const exercisePath = 'users/test-user/exerciseData/panca';
const legacyExercisePath = 'users/test-user/scheda/giorni/giorno1/gruppiMuscolari/gruppo1/esercizi/esercizio1';
const scheda = {
  dataInizio: '2026-09-01T10:00:00+0200', durata: 4, cambioRichiesto: false,
  giorni: { giorno1: { name: 'Giorno 1', gruppiMuscolari: { gruppo1: {
    nome: 'Petto', esercizi: { esercizio1: { name: 'Panca', serie: '3x10', riposo: '60', priorita: 1 } }
  } } } }
};
const user = { nome: 'Test', cognome: 'Locale', scheda };
const alert = { id: 'test-alert', titolo: 'Test', descrizione: 'Locale', urgenza: 'bassa', scadenza: 1800000000000 };
const png = new Uint8Array(Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=', 'base64'));
let env;
let guest;
let member;
let admin;

before(async () => {
  const databaseRules = await readFile(process.env.SPORTILI_DATABASE_RULES || new URL('../fixtures/published.database.rules.json', import.meta.url), 'utf8');
  const storageRules = await readFile(process.env.SPORTILI_STORAGE_RULES || new URL('../fixtures/published.storage.rules', import.meta.url), 'utf8');
  env = await initializeTestEnvironment({
    projectId,
    database: { host: '127.0.0.1', port: 19000, rules: databaseRules },
    storage: { host: '127.0.0.1', port: 19199, rules: storageRules }
  });
  guest = env.unauthenticatedContext();
  const claims = { firebase: { sign_in_provider: 'anonymous', identities: {} } };
  member = env.authenticatedContext('test-member-uid', claims);
  // The released admin also has anonymous Auth and no trusted role claim.
  admin = env.authenticatedContext('test-admin-uid', claims);
  await env.withSecurityRulesDisabled(async context => {
    await context.storage().ref('Panca.png').put(png, { contentType: 'image/png' });
  });
});

beforeEach(async () => {
  await env.withSecurityRulesDisabled(context => context.database().ref().set({
    fausto: 'fake-local-admin-code',
    users: { 'test-user': user },
    alerts: { 'test-alert': alert },
    esercizi: { Petto: { Panca: 'Panca' } }
  }));
});

after(async () => { await env?.cleanup(); });

test('iOS 1.5.3: reads fausto and the entire users node before Auth', async () => {
  const code = await assertSucceeds(guest.database().ref('fausto').once('value'));
  const users = await assertSucceeds(guest.database().ref('users').once('value'));
  assert.equal(code.val(), 'fake-local-admin-code');
  assert.equal(users.child('test-user/nome').val(), 'Test');
});

test('Android 1.3.4: anonymous login reads user and admin code', async () => {
  await assertSucceeds(member.database().ref('users/test-user').once('value'));
  await assertSucceeds(member.database().ref('fausto').once('value'));
});

test('both clients: profile, workout, exercise history, catalog and alerts remain readable', async () => {
  for (const path of ['users/test-user', 'users/test-user/scheda', 'users/test-user/exerciseData', 'esercizi', 'alerts']) {
    await assertSucceeds(member.database().ref(path).once('value'));
  }
});

test('both clients: create, update and delete a weight entry and note', async () => {
  const exercise = member.database().ref(exercisePath);
  const entry = exercise.child('weightLogs').push();
  await assertSucceeds(entry.set({ weight: 42.5, timestamp: 1790000000000 }));
  await assertSucceeds(entry.set({ weight: 45, timestamp: 1790000000000 }));
  await assertSucceeds(exercise.child('noteUtente').set('Nota di test'));
  assert.equal((await entry.once('value')).child('weight').val(), 45);
  await assertSucceeds(entry.remove());
  await assertSucceeds(exercise.child('noteUtente').remove());
  assert.equal((await exercise.once('value')).exists(), false);
});

test('iOS legacy notes and both clients workout-change request remain writable', async () => {
  const note = member.database().ref(`${legacyExercisePath}/noteUtente`);
  await assertSucceeds(note.set('Nota legacy'));
  await assertSucceeds(note.remove());
  await assertSucceeds(member.database().ref('users/test-user/scheda/cambioRichiesto').set(true));
});

test('released Android normalization can still replace the workout subtree', async () => {
  await assertSucceeds(member.database().ref('users/test-user/scheda').set(scheda));
});

test('released admin: list, create, replace and delete user without role claims', async () => {
  const users = admin.database().ref('users');
  await assertSucceeds(users.once('value'));
  await assertSucceeds(users.child('test-new-user').set(user));
  await assertSucceeds(users.child('test-new-user').set({ ...user, nome: 'Modificato' }));
  await assertSucceeds(users.child('test-new-user').remove());
});

test('updated admin: partial name update preserves workout and history', async () => {
  const users = admin.database().ref('users/test-user');
  await assertSucceeds(users.child('exerciseData/panca/noteUtente').set('Da conservare'));
  await assertSucceeds(users.update({ nome: 'Nuovo', cognome: 'Nome' }));
  const saved = await assertSucceeds(users.once('value'));
  assert.deepEqual(saved.child('scheda').val(), scheda);
  assert.equal(saved.child('exerciseData/panca/noteUtente').val(), 'Da conservare');
});

test('admin: workout, day, group and exercise replacements and deletions', async () => {
  const paths = ['users/test-user/scheda', 'users/test-user/scheda/giorni/giorno1', 'users/test-user/scheda/giorni/giorno1/gruppiMuscolari/gruppo1', legacyExercisePath];
  // Save real-shaped values, then delete bottom-up just as separate admin operations.
  for (const path of paths) {
    const ref = admin.database().ref(path);
    const snapshot = await assertSucceeds(ref.once('value'));
    assert.equal(snapshot.exists(), true);
    await assertSucceeds(ref.set(snapshot.val()));
  }
  for (const path of paths.reverse()) await assertSucceeds(admin.database().ref(path).remove());
});

test('admin: create, edit and delete alerts', async () => {
  const ref = admin.database().ref('alerts').push();
  await assertSucceeds(ref.set({ ...alert, id: ref.key }));
  await assertSucceeds(ref.set({ ...alert, id: ref.key, titolo: 'Aggiornato' }));
  await assertSucceeds(ref.remove());
});

test('Android member submits a report; anonymous admin reads, resolves and deletes it', async () => {
  const ref = member.database().ref('workoutIssueReports').push();
  const report = { id: ref.key, userCode: 'test-user', userName: 'Test', message: 'Test locale', createdAt: 1790000000000, resolved: false };
  await assertSucceeds(ref.set(report));
  await assertSucceeds(admin.database().ref('workoutIssueReports').once('value'));
  const adminRef = admin.database().ref(`workoutIssueReports/${ref.key}`);
  await assertSucceeds(adminRef.set({ ...report, resolved: true, resolutionNote: 'Risolto' }));
  await assertSucceeds(adminRef.remove());
});

test('exercise images: Storage read works without Auth and with anonymous Auth', async () => {
  // Metadata requests evaluate read rules. Production download tokens/CDN are not simulated.
  for (const context of [guest, member]) {
    const metadata = await assertSucceeds(context.storage().ref('Panca.png').getMetadata());
    assert.equal(metadata.contentType, 'image/png');
  }
});
