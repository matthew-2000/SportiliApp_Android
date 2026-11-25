"""Direct Google Play production release. Local preparation is the default."""
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


def release_parameters(env, root):
    code, name = env.get("SPORTILI_VERSION_CODE", ""), env.get("SPORTILI_VERSION_NAME", "")
    if not re.fullmatch(r"[1-9][0-9]*", code) or int(code) > 2_100_000_000:
        raise ValueError("Invalid versionCode")
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", name):
        raise ValueError("Use major.minor.patch")
    percent = int(env.get("ROLLOUT_PERCENT", "100"))
    if not 1 <= percent <= 100:
        raise ValueError("Rollout must be 1..100 percent")
    path = (root / env.get("NOTES_FILE", "")).resolve()
    if not path.is_relative_to((root / "release-notes").resolve()):
        raise ValueError("Notes must be under release-notes/")
    notes = (json.loads(path.read_text())["notes"] if path.suffix == ".json" else path.read_text()).strip()
    if not 1 <= len(notes) <= 500:
        raise ValueError("Release notes must contain 1..500 characters")
    release = {"name": name, "versionCodes": [code],
               "status": "completed" if percent == 100 else "inProgress",
               "releaseNotes": [{"language": "it-IT", "text": notes}]}
    if percent < 100:
        release["userFraction"] = percent / 100
    return {"track": "production", "releases": [release]}


def prepare(track, bundle, commit):
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("Expected full commit SHA")
    if track.get("track") != "production" or len(track.get("releases", [])) != 1:
        raise ValueError("Expected one production release")
    return {"package": PACKAGE, "commit": commit, "track": track,
            "sha256": hashlib.sha256(bundle.read_bytes()).hexdigest()}


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
        raw = response.read()
        return json.loads(raw) if raw else {}


def publish(plan, bundle, commit, token, call=request):
    track = plan["track"]
    if plan != prepare(track, bundle, commit):
        raise ValueError("Bundle, commit or release plan changed after preparation")
    if track["releases"][0]["status"] not in ("completed", "inProgress"):
        raise ValueError("Invalid production status")
    edit = call("POST", API, token, payload={})["id"]
    committed = False
    try:
        current = call("GET", f"{API}/{edit}/tracks/production", token)
        if any(r.get("status") in ("inProgress", "halted") for r in current.get("releases", [])):
            raise ValueError("An existing production rollout must be resolved before replacing it")
        bundles = call("GET", f"{API}/{edit}/bundles", token)
        code = track["releases"][0]["versionCodes"][0]
        if any(int(b["versionCode"]) >= int(code) for b in bundles.get("bundles", [])):
            raise ValueError("versionCode must exceed previously uploaded bundles")
        uploaded = call("POST", f"{UPLOAD}/{edit}/bundles?uploadType=media", token, media=bundle.read_bytes())
        if str(uploaded["versionCode"]) != code or uploaded.get("sha256", "").lower() != plan["sha256"]:
            raise ValueError("Uploaded bundle version or checksum mismatch")
        call("PUT", f"{API}/{edit}/tracks/production", token, payload=track)
        call("POST", f"{API}/{edit}:validate", token)
        call("POST", f"{API}/{edit}:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW", token)
        committed = True
        return plan | {"editId": edit, "result": "submitted-to-google-play"}
    finally:
        if not committed:
            try:
                call("DELETE", f"{API}/{edit}", token)
            except Exception:
                pass


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare", action="store_true")
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    if args.prepare and args.publish:
        parser.error("Choose prepare or publish")
    root = Path(__file__).resolve().parents[1]
    bundle = root / "dist/app-release.aab"
    plan_file = root / "dist/release-plan.json"
    if args.publish:
        receipt = publish(json.loads(plan_file.read_text()), bundle, os.environ["GITHUB_SHA"],
                          os.environ["GOOGLE_PLAY_ACCESS_TOKEN"])
        (root / "dist/play-production-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
        print("Production release submitted. Availability depends on Google review and publishing settings.")
    else:
        track = release_parameters(os.environ, root)
        if args.prepare:
            plan_file.write_text(json.dumps(prepare(track, bundle, os.environ["GITHUB_SHA"]), indent=2) + "\n")
        print(json.dumps(track, ensure_ascii=False, indent=2))
        print("Local preparation only; no Google API requests.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError) as error:
        raise SystemExit(f"Production release failed: {error}") from None
