#!/usr/bin/env python3
import argparse
import base64
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


DEFAULT_MODEL = "gpt-image-1"
DEFAULT_SIZE = "1024x1024"
DEFAULT_QUALITY = "medium"
DEFAULT_FORMAT = "png"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Genera immagini esercizi in batch a partire da un file di prompt strutturato."
    )
    parser.add_argument(
        "--input",
        required=True,
        help="File di testo con il formato Categoria + righe che iniziano con '* '.",
    )
    parser.add_argument(
        "--output-dir",
        default="output/exercise-images",
        help="Cartella dove salvare le immagini generate.",
    )
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--size", default=DEFAULT_SIZE)
    parser.add_argument("--quality", default=DEFAULT_QUALITY)
    parser.add_argument("--format", default=DEFAULT_FORMAT, choices=["png", "jpeg", "webp"])
    parser.add_argument(
        "--background",
        default="opaque",
        choices=["opaque", "transparent", "auto"],
    )
    parser.add_argument(
        "--compression",
        type=int,
        default=None,
        help="Compressione 0-100 per jpeg/webp.",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Genera solo i primi N prompt utili per fare test.",
    )
    parser.add_argument(
        "--skip-existing",
        action="store_true",
        help="Salta i file già presenti.",
    )
    parser.add_argument(
        "--delay-seconds",
        type=float,
        default=0.0,
        help="Pausa tra richieste consecutive.",
    )
    return parser.parse_args()


def slugify(value: str) -> str:
    normalized = value.strip().lower()
    normalized = normalized.replace("&", " e ")
    normalized = re.sub(r"[\/+]", " ", normalized)
    normalized = re.sub(r"[^a-z0-9àèéìòù]+", "_", normalized, flags=re.IGNORECASE)
    normalized = re.sub(r"_+", "_", normalized)
    normalized = normalized.strip("_")
    return normalized or "exercise"


def extract_exercise_name(prompt: str) -> str:
    patterns = [
        r"mentre esegue\s+(.+?)\s+con attrezzatura",
        r"mentre esegue\s+(.+?),\s*nello stesso stile",
        r"mentre esegue\s+(.+?)\s+nello stesso stile",
    ]
    for pattern in patterns:
        match = re.search(pattern, prompt, flags=re.IGNORECASE)
        if match:
            return match.group(1).strip().rstrip(".")
    return prompt[:80].strip().rstrip(".")


def parse_prompt_file(path: Path) -> list[dict]:
    items = []
    current_category = None
    lines = path.read_text(encoding="utf-8").splitlines()
    for index, raw_line in enumerate(lines, start=1):
        line = raw_line.strip()
        if not line:
            continue
        if line.startswith("* "):
            if current_category is None:
                raise ValueError(f"Prompt senza categoria alla riga {index}")
            prompt = line[2:].strip()
            exercise_name = extract_exercise_name(prompt)
            items.append(
                {
                    "category": current_category,
                    "exercise_name": exercise_name,
                    "prompt": prompt,
                    "line": index,
                }
            )
            continue
        current_category = line
    return items


def build_output_path(output_dir: Path, category: str, exercise_name: str, image_format: str) -> Path:
    category_dir = output_dir / slugify(category)
    category_dir.mkdir(parents=True, exist_ok=True)
    filename = f"{slugify(exercise_name)}.{image_format}"
    return category_dir / filename


def call_openai_image_api(
    api_key: str,
    prompt: str,
    *,
    model: str,
    size: str,
    quality: str,
    image_format: str,
    background: str,
    compression: int | None,
) -> bytes:
    body = {
        "model": model,
        "prompt": prompt,
        "size": size,
        "quality": quality,
        "output_format": image_format,
        "background": background,
    }
    if compression is not None:
        body["output_compression"] = compression

    request = urllib.request.Request(
        url="https://api.openai.com/v1/images/generations",
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    with urllib.request.urlopen(request, timeout=300) as response:
        payload = json.loads(response.read().decode("utf-8"))

    image_base64 = payload["data"][0]["b64_json"]
    return base64.b64decode(image_base64)


def write_manifest(manifest_path: Path, rows: list[dict]) -> None:
    manifest_path.write_text(
        json.dumps(rows, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def main() -> int:
    args = parse_args()
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        print("Manca OPENAI_API_KEY nell'ambiente.", file=sys.stderr)
        return 1

    input_path = Path(args.input)
    if not input_path.exists():
        print(f"File input non trovato: {input_path}", file=sys.stderr)
        return 1

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    items = parse_prompt_file(input_path)
    if args.limit is not None:
        items = items[: args.limit]

    if not items:
        print("Nessun prompt trovato.", file=sys.stderr)
        return 1

    manifest_rows = []
    failures = 0

    for index, item in enumerate(items, start=1):
        output_path = build_output_path(
            output_dir=output_dir,
            category=item["category"],
            exercise_name=item["exercise_name"],
            image_format=args.format,
        )

        if args.skip_existing and output_path.exists():
            print(f"[{index}/{len(items)}] SKIP {output_path}")
            manifest_rows.append(
                {
                    **item,
                    "output_path": str(output_path),
                    "status": "skipped_existing",
                }
            )
            continue

        print(f"[{index}/{len(items)}] Genero {item['exercise_name']} -> {output_path}")
        try:
            image_bytes = call_openai_image_api(
                api_key=api_key,
                prompt=item["prompt"],
                model=args.model,
                size=args.size,
                quality=args.quality,
                image_format=args.format,
                background=args.background,
                compression=args.compression,
            )
            output_path.write_bytes(image_bytes)
            manifest_rows.append(
                {
                    **item,
                    "output_path": str(output_path),
                    "status": "generated",
                }
            )
        except urllib.error.HTTPError as error:
            body = error.read().decode("utf-8", errors="replace")
            failures += 1
            print(
                f"[{index}/{len(items)}] ERRORE HTTP {error.code} per '{item['exercise_name']}': {body}",
                file=sys.stderr,
            )
            manifest_rows.append(
                {
                    **item,
                    "output_path": str(output_path),
                    "status": "http_error",
                    "error": body,
                }
            )
        except Exception as error:
            failures += 1
            print(
                f"[{index}/{len(items)}] ERRORE per '{item['exercise_name']}': {error}",
                file=sys.stderr,
            )
            manifest_rows.append(
                {
                    **item,
                    "output_path": str(output_path),
                    "status": "error",
                    "error": str(error),
                }
            )

        if args.delay_seconds > 0:
            time.sleep(args.delay_seconds)

    manifest_path = output_dir / "manifest.json"
    write_manifest(manifest_path, manifest_rows)
    print(f"Manifest scritto in {manifest_path}")

    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
