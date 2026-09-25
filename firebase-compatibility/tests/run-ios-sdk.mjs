import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';

const env = await initializeTestEnvironment({
  projectId: 'demo-sportili-compat',
  database: {
    host: '127.0.0.1', port: 19000,
    rules: JSON.stringify({ rules: {
      fausto: { '.read': true },
      users: { '$code': { '.read': "$code !== 'read-denied'", '.write': true } },
      integrationResults: { '.read': true, '.write': true }
    } })
  }
});
try {
  await env.withSecurityRulesDisabled(async context => {
    await context.database().ref().set({
      fausto: 'admin-local',
      users: {
        valid: { nome: 'Valida', cognome: 'Test' },
        A: { scheda: { dataInizio: '2026-09-01T12:00:00+0200', durata: 4 } },
        B: { scheda: { dataInizio: '2026-09-02T12:00:00+0200', durata: 6 } },
        notes: { exerciseData: { panca: {
          noteUtente: 'prima', future: 'keep',
          weightLogs: { log1: { weight: 40, timestamp: 1790000000000 } }
        } } }
      }
    });
  });
  const iosRoot = fileURLToPath(new URL('../../../SportiliApp_iOS/', import.meta.url));
  const args = ['Tests/run_firebase_sdk_tests.py'];
  if (process.env.IOS_SIMULATOR_UDID) args.push('--simulator', process.env.IOS_SIMULATOR_UDID);
  const result = spawnSync('python3', args, { cwd: iosRoot, stdio: 'inherit' });
  if (result.error) throw result.error;
  process.exitCode = result.status ?? 1;
} finally {
  await env.cleanup();
}
