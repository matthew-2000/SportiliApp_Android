"""Exercise the Play edit transaction using a fake API; never contact Google."""
import hashlib
from pathlib import Path
import tempfile
import unittest

from publish_internal import publish, release_parameters


class InternalReleaseTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        (self.root / "release-notes").mkdir()
        (self.root / "release-notes/it-IT.txt").write_text("Correzioni e miglioramenti.")
        self.env = {"SPORTILI_VERSION_CODE": "37", "SPORTILI_VERSION_NAME": "2.0.4",
                    "NOTES_FILE": "release-notes/it-IT.txt"}
        self.track = release_parameters(self.env, self.root)
        self.bundle = self.root / "app.aab"
        self.bundle.write_bytes(b"fake signed bundle")
        self.calls = []

    def api(self, method, url, token, **kwargs):
        self.calls.append((method, url, kwargs))
        if url.endswith("/edits"):
            return {"id": "fake-edit"}
        if "/bundles?" in url:
            return {"versionCode": 37, "sha256": hashlib.sha256(self.bundle.read_bytes()).hexdigest()}
        return {}

    def test_internal_transaction_and_receipt(self):
        receipt = publish(self.track, self.bundle, "fake-token", self.api)
        self.assertEqual(receipt["track"], "internal")
        self.assertEqual(receipt["versionCode"], "37")
        self.assertEqual([c[0] for c in self.calls], ["POST", "POST", "PUT", "POST", "POST"])
        self.assertTrue(self.calls[2][1].endswith("/tracks/internal"))
        self.assertIn("changesInReviewBehavior=ERROR_IF_IN_REVIEW", self.calls[-1][1])

    def test_reject_other_tracks_before_any_request(self):
        self.track["track"] = "production"
        with self.assertRaises(ValueError):
            publish(self.track, self.bundle, "fake-token", self.api)
        self.assertEqual(self.calls, [])

    def test_bad_upload_is_deleted_without_commit(self):
        for replacement in ({"versionCode": 38}, {"sha256": "wrong"}):
            with self.subTest(replacement=replacement):
                self.calls.clear()
                def bad_upload(method, url, token, **kwargs):
                    result = self.api(method, url, token, **kwargs)
                    return result | replacement if "/bundles?" in url else result
                with self.assertRaises(ValueError):
                    publish(self.track, self.bundle, "fake-token", bad_upload)
                self.assertEqual(self.calls[-1][0], "DELETE")
                self.assertFalse(any(":commit" in c[1] for c in self.calls))

    def test_failed_validation_preserves_error_and_deletes_edit(self):
        def failed_validation(method, url, token, **kwargs):
            result = self.api(method, url, token, **kwargs)
            if url.endswith(":validate"):
                raise RuntimeError("validation rejected")
            return result
        with self.assertRaisesRegex(RuntimeError, "validation rejected"):
            publish(self.track, self.bundle, "fake-token", failed_validation)
        self.assertEqual(self.calls[-1][0], "DELETE")
        self.assertFalse(any(":commit" in c[1] for c in self.calls))

    def test_invalid_parameters(self):
        for key, value in (("SPORTILI_VERSION_CODE", "0"), ("SPORTILI_VERSION_CODE", "2100000001"),
                           ("SPORTILI_VERSION_NAME", "bad"), ("NOTES_FILE", "../outside.txt")):
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                release_parameters(self.env | {key: value}, self.root)

    def test_empty_or_long_notes(self):
        for content in ("", "a" * 501):
            (self.root / self.env["NOTES_FILE"]).write_text(content)
            with self.assertRaises(ValueError):
                release_parameters(self.env, self.root)


if __name__ == "__main__":
    unittest.main()
