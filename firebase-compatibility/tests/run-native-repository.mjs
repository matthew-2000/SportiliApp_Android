import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';

// Only a named test user is writable. The second test exercises real permission denial.
const env = await initializeTestEnvironment({
  projectId: 'demo-sportili-compat',
  database: {
    host: '127.0.0.1', port: 19000,
    rules: JSON.stringify({ rules: { '.read': true, '.write': false, users: { 'native-test': { '.write': true } } } })
  }
});
try {
  const result = spawnSync('./gradlew', [
    ':app:connectedDebugAndroidTest', '--console=plain',
    '-Pandroid.testInstrumentationRunnerArguments.class=com.matthew.sportiliapp.newadmin.FirebaseRepositoryEmulatorTest',
    '-Pandroid.testInstrumentationRunnerArguments.sportiliLocalDatabase=1'
  ], { cwd: fileURLToPath(new URL('../../', import.meta.url)), stdio: 'inherit' });
  if (result.error) throw result.error;
  process.exitCode = result.status ?? 1;
} finally {
  await env.cleanup();
}
