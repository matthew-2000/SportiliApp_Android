import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';

// Local emulator only: synthetic users writable except the permission-denial fixture.
const env = await initializeTestEnvironment({
  projectId: 'demo-sportili-compat',
  database: {
    host: '127.0.0.1', port: 19000,
    rules: JSON.stringify({ rules: { '.read': false, '.write': false, users: { '$code': { '.read': "$code !== 'read-denied'", '.write': "$code !== 'denied'" } } } })
  }
});
const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT ||
  readFileSync(new URL('../../local.properties', import.meta.url), 'utf8').match(/^sdk.dir=(.+)$/m)?.[1];
const adb = sdk ? join(sdk, 'platform-tools/adb') : 'adb';
let serial;
let createdReverse = false;
function adbRun(args) {
  const result = spawnSync(adb, args, { encoding: 'utf8' });
  if (result.error || result.status !== 0) throw result.error || new Error(result.stderr);
  return result.stdout;
}
try {
  const emulators = adbRun(['devices']).split('\n').map(line => line.match(/^(emulator-\d+)\s+device$/)?.[1]).filter(Boolean);
  serial = process.env.ANDROID_SERIAL || (emulators.length === 1 ? emulators[0] : null);
  if (!serial || !emulators.includes(serial)) throw new Error('Select one running Android Emulator with ANDROID_SERIAL. Physical devices are not supported.');
  // RTDB advertises 127.0.0.1 in its handshake, including after reconnect.
  const mappings = adbRun(['-s', serial, 'reverse', '--list']);
  const existing = mappings.split('\n').find(line => line.split(/\s+/)[1] === 'tcp:19000');
  if (existing && existing.split(/\s+/)[2] !== 'tcp:19000') throw new Error('Port 19000 already has a different adb reverse mapping.');
  if (!existing) {
    adbRun(['-s', serial, 'reverse', 'tcp:19000', 'tcp:19000']);
    createdReverse = true;
  }
  const result = spawnSync('./gradlew', [
    ':app:connectedDebugAndroidTest', '--console=plain',
    '-Pandroid.testInstrumentationRunnerArguments.class=com.matthew.sportiliapp.newadmin.FirebaseRepositoryEmulatorTest,com.matthew.sportiliapp.model.SchedaRealtimeEmulatorTest',
    '-Pandroid.testInstrumentationRunnerArguments.sportiliLocalDatabase=1'
  ], { cwd: fileURLToPath(new URL('../../', import.meta.url)), stdio: 'inherit', env: { ...process.env, ANDROID_SERIAL: serial } });
  if (result.error) throw result.error;
  process.exitCode = result.status ?? 1;
} finally {
  if (createdReverse) adbRun(['-s', serial, 'reverse', '--remove', 'tcp:19000']);
  await env.cleanup();
}
