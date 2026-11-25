"""Resolve a release tag or manual inputs, restricted to the current main commit."""
import json
import os
from pathlib import Path
import re
import subprocess
from publish_production import release_parameters


def resolve(env, root, main_sha, head_sha):
    if env["GITHUB_SHA"] != main_sha or head_sha != main_sha:
        raise ValueError("Release must point to the current main commit")
    if env["GITHUB_EVENT_NAME"] == "push":
        match = re.fullmatch(r"refs/tags/v(\d+\.\d+\.\d+)", env["GITHUB_REF"])
        if not match:
            raise ValueError("Expected a version tag vX.Y.Z")
        version = match[1]
        notes = f"release-notes/{version}.json"
        config = json.loads((root / notes).read_text())
        if not isinstance(config.get("version_code"), int) or isinstance(config["version_code"], bool):
            raise ValueError("Tag release config requires integer version_code")
        values = {"SPORTILI_VERSION_NAME": version, "SPORTILI_VERSION_CODE": str(config["version_code"]),
                  "NOTES_FILE": notes, "ROLLOUT_PERCENT": str(config.get("rollout_percent", 100))}
    elif env["GITHUB_EVENT_NAME"] == "workflow_dispatch" and env["GITHUB_REF"] == "refs/heads/main":
        values = {k: env[k] for k in ("SPORTILI_VERSION_NAME", "SPORTILI_VERSION_CODE", "NOTES_FILE", "ROLLOUT_PERCENT")}
    else:
        raise ValueError("Unsupported release event or branch")
    release_parameters(values, root)
    return {"version_name": values["SPORTILI_VERSION_NAME"], "version_code": values["SPORTILI_VERSION_CODE"],
            "notes_file": values["NOTES_FILE"], "rollout_percent": values["ROLLOUT_PERCENT"]}


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1]
    rev = lambda ref: subprocess.check_output(["git", "rev-parse", ref], text=True).strip()
    outputs = resolve(os.environ, root, rev("origin/main"), rev("HEAD"))
    with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
        for key, value in outputs.items():
            if "\n" in value or "\r" in value:
                raise ValueError("Multiline release input rejected")
            stream.write(f"{key}={value}\n")
