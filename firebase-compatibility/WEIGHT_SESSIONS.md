# Multi-series weights

The shared additive format is `users/{code}/weightSessions/{canonicalExerciseKey}/{sessionId}`:

```json
{
  "schemaVersion": 1,
  "timestamp": 1790000000000,
  "sets": {
    "set_00": { "reps": 12, "weight": 30 },
    "set_01": { "reps": 10, "weight": 35.5 },
    "set_02": { "reps": 8, "weight": 40 },
    "set_03": { "reps": 6, "weight": 45 }
  }
}
```

- Timestamp is integer milliseconds and survives edits; a draft keeps its ID on retry.
- Sets use contiguous padded map keys, not Firebase numeric-key arrays. There are 1–20 sets, integer reps 1–1000, and finite weights greater than zero.
- Use `ExerciseDataKey.canonical` for sessions, independently of legacy aliases. Do not migrate or rewrite `exerciseData` keys.
- Legacy `weightLogs` and notes remain unchanged. Installed clients keep working with single weights; they do not display new multi-series sessions. Released iOS cleanup of an `exerciseData` node cannot delete this sibling subtree.
- Updated clients merge histories by date. Charts use only single weights; reuse and series comparisons require identical ordered reps. Old weights never acquire inferred reps.
- Auto-fill plain `12-10-8-6` / `4x10`; composite instructions and supersets require manual series entry.
- Profile and workout edits preserve sessions. Deleting a user intentionally deletes all of their data, as before.
- No production rules, Auth, Storage or admin changes are deployed. Recorded-rule compatibility is tested locally.

Checks:

```sh
./gradlew :app:testDebugUnitTest --tests com.matthew.sportiliapp.model.WorkoutWeightSessionTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.matthew.sportiliapp.WeightSessionUiTest
cd firebase-compatibility
npx -y firebase-tools@latest emulators:exec --project demo-sportili-compat --config firebase.emulators.json --only database 'node --test tests/weight-sessions.test.mjs'
SPORTILI_NATIVE_TEST_CLASS=com.matthew.sportiliapp.model.SchedaRealtimeEmulatorTest npm run test:android-repository
```

The native repository runner requires one running Android emulator. It uses demo Firebase apps, isolated preferences and loopback RTDB; it never accesses production.
