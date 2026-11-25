import json
from pathlib import Path
import tempfile
import unittest
from release_inputs import resolve


class ReleaseInputTests(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        (self.root / "release-notes").mkdir()
        (self.root / "release-notes/2.0.4.json").write_text(json.dumps({"version_code": 37, "notes": "Novità"}))
        self.env = {"GITHUB_SHA": "a" * 40, "GITHUB_EVENT_NAME": "push", "GITHUB_REF": "refs/tags/v2.0.4"}

    def test_tag_reads_committed_release_config(self):
        self.assertEqual(resolve(self.env, self.root, "a" * 40, "a" * 40), {
            "version_name": "2.0.4", "version_code": "37", "notes_file": "release-notes/2.0.4.json", "rollout_percent": "100"})

    def test_other_commits_or_invalid_tag_are_rejected(self):
        for change in ({"GITHUB_SHA": "b" * 40}, {"GITHUB_REF": "refs/tags/v2.0.4-rc1"}, {"GITHUB_REF": "refs/heads/feature"}):
            with self.subTest(change=change), self.assertRaises(ValueError):
                resolve(self.env | change, self.root, "a" * 40, "a" * 40)

    def test_manual_main_release(self):
        env = self.env | {"GITHUB_EVENT_NAME": "workflow_dispatch", "GITHUB_REF": "refs/heads/main",
            "SPORTILI_VERSION_NAME": "2.0.4", "SPORTILI_VERSION_CODE": "37", "NOTES_FILE": "release-notes/2.0.4.json", "ROLLOUT_PERCENT": "10"}
        self.assertEqual(resolve(env, self.root, "a" * 40, "a" * 40)["rollout_percent"], "10")
