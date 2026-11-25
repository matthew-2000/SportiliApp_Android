"""Google Play API upload, restricted to internal testing. Dry run by default."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import urllib.error
import urllib.request

PACKAGE = "com.matthew.sportiliapp"
API = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}/edits"
UPLOAD = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PACKAGE}/edits"


def release_parameters(environ, root):
    code = environ.get("SPORTILI_VERSION_CODE", "")
    name = environ.get("SPORTILI_VERSION_NAME", "")
    if not re.fullmatch(r"[1-9][0-9]*", code) or int(code) > 2_100_000_000:
        raise ValueError("Version code must be a positive integer <= 2100000000")
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", name):
        raise ValueError("Use a version name in major.minor.patch format")
    notes_path = (root / environ.get("NOTES_FILE", "")).resolve()
    if not notes_path.is_relative_to((root / "release-notes").resolve()):
        raise ValueError("Notes must be a tracked file under release-notes/")
    notes = notes_path.read_text().strip()
    if not notes or len(notes) > 500:
        raise ValueError("Italian release notes must contain 1 to 500 characters")
    return {
        "track": "internal",
        "releases": [{
            "name": name,
            "versionCodes": [code],
            "status": "completed",
            "releaseNotes": [{"language": "it-IT", "text": notes}],
        }],
    }


def request(method, url, token, payload=None, media=None):
    headers = {"Authorization": f"Bearer {token}"}
    body = None
    if payload is not None:
        body = json.dumps(payload).encode()
        headers["Content-Type"] = "application/json"
    elif media is not None:
        body = media
        headers["Content-Type"] = "application/octet-stream"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=600) as response:
        return json.load(response)


def publish(track, bundle, token, call=request):
    if track.get("track") != "internal":
        raise ValueError("Only the internal testing track is allowed")
    bundle_data = bundle.read_bytes()
    digest = hashlib.sha256(bundle_data).hexdigest()
    edit = call("POST", API, token, payload={})["id"]
    committed = False
    try:
        uploaded = call("POST", f"{UPLOAD}/{edit}/bundles?uploadType=media", token, media=bundle_data)
        expected = track["releases"][0]["versionCodes"][0]
        if str(uploaded["versionCode"]) != expected:
            raise ValueError("Uploaded bundle version differs from requested version; edit not committed")
        if uploaded.get("sha256", "").lower() != digest:
            raise ValueError("Uploaded bundle checksum mismatch; edit not committed")
        call("PUT", f"{API}/{edit}/tracks/internal", token, payload=track)
        call("POST", f"{API}/{edit}:validate", token)
        call("POST", f"{API}/{edit}:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW", token)
        committed = True
        return {"package": PACKAGE, "track": "internal", "versionCode": expected,
                "sha256": digest, "commit": os.environ.get("GITHUB_SHA"), "editId": edit}
    finally:
        if not committed:
            try:
                call("DELETE", f"{API}/{edit}", token)
            except Exception:
                # Preserve the original failure; an uncommitted edit expires.
                pass


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    track = release_parameters(os.environ, root)
    if not args.publish:
        print("Dry run: internal testing only; no Google API requests.")
        print(json.dumps(track, ensure_ascii=False, indent=2))
        return
    token = os.environ.get("GOOGLE_PLAY_ACCESS_TOKEN")
    if not token:
        raise ValueError("Missing Google Play access token")
    receipt = publish(track, root / "app/build/outputs/bundle/release/app-release.aab", token)
    (root / "play-internal-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("Internal test release committed. No production track was modified.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError) as error:
        # HTTP errors report the status, without printing tokens or credential bodies.
        raise SystemExit(f"Internal publishing failed: {error}") from None
