import hashlib
from pathlib import Path
import tempfile
import unittest
from publish_production import prepare, publish, release_parameters


class ProductionTests(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory()
        self.addCleanup(folder.cleanup)
        self.root = Path(folder.name)
        (self.root / "release-notes").mkdir()
        (self.root / "release-notes/it.txt").write_text("Miglioramenti.")
        self.env = {"SPORTILI_VERSION_CODE": "37", "SPORTILI_VERSION_NAME": "2.0.4",
                    "NOTES_FILE": "release-notes/it.txt"}
        self.bundle = self.root / "app.aab"
        self.bundle.write_bytes(b"signed-bundle")
        self.commit = "a" * 40
        self.plan = prepare(release_parameters(self.env, self.root), self.bundle, self.commit)
        self.calls = []

    def api(self, method, url, token, **kwargs):
        self.calls.append((method, url, kwargs))
        if url.endswith("/edits"):
            return {"id": "edit"}
        if "/bundles?" in url:
            return {"versionCode": 37, "sha256": hashlib.sha256(self.bundle.read_bytes()).hexdigest()}
        return {}

    def test_direct_production_transaction(self):
        result = publish(self.plan, self.bundle, self.commit, "token", self.api)
        self.assertEqual(result["result"], "submitted-to-google-play")
        self.assertEqual([c[0] for c in self.calls], ["POST", "GET", "GET", "POST", "PUT", "POST", "POST"])
        self.assertTrue(self.calls[4][1].endswith("/tracks/production"))
        self.assertIn("ERROR_IF_IN_REVIEW", self.calls[-1][1])
        self.assertFalse(any("internal" in c[1] for c in self.calls))

    def test_full_and_staged_rollout(self):
        full = self.plan["track"]["releases"][0]
        self.assertEqual(full["status"], "completed")
        self.assertNotIn("userFraction", full)
        staged = release_parameters(self.env | {"ROLLOUT_PERCENT": "10"}, self.root)["releases"][0]
        self.assertEqual((staged["status"], staged["userFraction"]), ("inProgress", .1))

    def test_changed_commit_bundle_or_track_prevents_requests(self):
        with self.assertRaises(ValueError):
            publish(self.plan, self.bundle, "b" * 40, "token", self.api)
        self.bundle.write_bytes(b"tampered")
        with self.assertRaises(ValueError):
            publish(self.plan, self.bundle, self.commit, "token", self.api)
        self.plan["track"]["track"] = "internal"
        with self.assertRaises(ValueError):
            publish(self.plan, self.bundle, self.commit, "token", self.api)
        self.assertEqual(self.calls, [])

    def test_existing_rollout_or_old_code_blocks_upload(self):
        for response in ({"releases": [{"status": "halted"}]}, {"bundles": [{"versionCode": 37}]}):
            with self.subTest(response=response):
                self.calls.clear()
                def guarded(method, url, token, **kwargs):
                    result = self.api(method, url, token, **kwargs)
                    return result | response if method == "GET" else result
                with self.assertRaises(ValueError):
                    publish(self.plan, self.bundle, self.commit, "token", guarded)
                self.assertEqual(self.calls[-1][0], "DELETE")
                self.assertFalse(any("uploadType" in c[1] for c in self.calls))

    def test_upload_mismatch_does_not_update_or_commit(self):
        def bad(method, url, token, **kwargs):
            result = self.api(method, url, token, **kwargs)
            return result | {"sha256": "wrong"} if "uploadType" in url else result
        with self.assertRaises(ValueError):
            publish(self.plan, self.bundle, self.commit, "token", bad)
        self.assertEqual(self.calls[-1][0], "DELETE")
        self.assertFalse(any(c[0] == "PUT" or ":commit" in c[1] for c in self.calls))

    def test_failed_commit_preserves_error_and_cleans_edit(self):
        def fail(method, url, token, **kwargs):
            result = self.api(method, url, token, **kwargs)
            if ":commit" in url:
                raise RuntimeError("review already in progress")
            return result
        with self.assertRaisesRegex(RuntimeError, "review already"):
            publish(self.plan, self.bundle, self.commit, "token", fail)
        self.assertEqual(self.calls[-1][0], "DELETE")

    def test_invalid_parameters(self):
        for key, value in (("SPORTILI_VERSION_CODE", "0"), ("SPORTILI_VERSION_CODE", "2100000001"),
                           ("SPORTILI_VERSION_NAME", "bad"), ("NOTES_FILE", "../x"),
                           ("ROLLOUT_PERCENT", "0"), ("ROLLOUT_PERCENT", "101")):
            with self.subTest(key=key), self.assertRaises(ValueError):
                release_parameters(self.env | {key: value}, self.root)


if __name__ == "__main__":
    unittest.main()
