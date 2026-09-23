import { readFile } from 'node:fs/promises';
import { test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';

// These tests demonstrate known exposure and unsafe migration shortcuts.
// Passing them does NOT mean the published rules are secure.
const publishedDatabase = await readFile(new URL('../fixtures/published.database.rules.json', import.meta.url), 'utf8');
const publishedStorage = await readFile(new URL('../fixtures/published.storage.rules', import.meta.url), 'utf8');
const anonymousClaims = { firebase: { sign_in_provider: 'anonymous', identities: {} } };

async function environment(t, rules = publishedDatabase) {
  const env = await initializeTestEnvironment({
    projectId: 'demo-sportili-compat',
    database: { host: '127.0.0.1', port: 19000, rules },
    storage: { host: '127.0.0.1', port: 19199, rules: publishedStorage }
  });
  t.after(() => env.cleanup());
  await env.withSecurityRulesDisabled(context => context.database().ref().set({
    fausto: 'fake-local-admin-code', users: { otherUser: { nome: 'Solo test', cognome: 'Locale' } }
  }));
  return env;
}

test('known exposure: unauthenticated callers can read users and alter another user', async t => {
  const env = await environment(t);
  const db = env.unauthenticatedContext().database();
  await assertSucceeds(db.ref('users').once('value'));
  await assertSucceeds(db.ref('users/otherUser').update({ nome: 'Alterato localmente' }));
  await assertSucceeds(db.ref('fausto').set('another-fake-local-code'));
});

test('known exposure: any anonymous Auth user can create, overwrite and delete a Storage object', async t => {
  const env = await environment(t);
  const path = 'boundary-test.png';
  const data = new Uint8Array([1, 2, 3]);
  await assertFails(env.unauthenticatedContext().storage().ref(path).put(data));
  const object = env.authenticatedContext('unrelated-user', anonymousClaims).storage().ref(path);
  await assertSucceeds(object.put(data));
  await assertSucceeds(object.put(new Uint8Array([4, 5])));
  await assertSucceeds(object.delete());
});

test('rejected shortcut: auth-only reads block installed iOS login but still expose users to anonymous Auth', async t => {
  const env = await environment(t, JSON.stringify({ rules: { '.read': 'auth != null', '.write': 'auth != null' } }));
  const guest = env.unauthenticatedContext().database();
  await assertFails(guest.ref('fausto').once('value'));
  await assertFails(guest.ref('users').once('value'));
  await assertSucceeds(env.authenticatedContext('any-anonymous-user', anonymousClaims).database().ref('users').once('value'));
});

test('rejected shortcut: requiring an admin claim immediately blocks released Android admin writes', async t => {
  const env = await environment(t, JSON.stringify({ rules: { '.read': true, '.write': 'auth != null && auth.token.admin === true' } }));
  const db = env.authenticatedContext('legacy-admin', anonymousClaims).database();
  await assertFails(db.ref('users/new-user').set({ nome: 'Nuovo', cognome: 'Test' }));
  await assertFails(db.ref('alerts/test').set({ titolo: 'Test', descrizione: 'Locale' }));
});

test('rejected shortcut: restrictive child rules cannot override public root grants', async t => {
  const env = await environment(t, JSON.stringify({ rules: {
    '.read': true, '.write': true,
    privateAccess: { '.read': false, '.write': false }
  } }));
  const privateRef = env.unauthenticatedContext().database().ref('privateAccess/test');
  await assertSucceeds(privateRef.set({ role: 'admin' }));
  await assertSucceeds(privateRef.once('value'));
});
