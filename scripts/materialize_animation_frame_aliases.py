#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
MAP = DOCS / "animation_frame_aliases.json"


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> None:
    data = json.loads(MAP.read_text(encoding="utf-8"))
    changed = 0
    verified = 0

    for item in data.get("aliases", []):
        source = DOCS / item["source"]
        target = DOCS / item["canonical"]
        if not source.is_file():
            raise FileNotFoundError(f"Missing alias source: {source}")

        if target.exists():
            if digest(source) != digest(target):
                raise RuntimeError(
                    f"Canonical target already exists with different content: {target}"
                )
            verified += 1
            continue

        shutil.copy2(source, target)
        changed += 1
        print(f"materialized {target.relative_to(ROOT)} <- {source.relative_to(ROOT)}")

    print(f"Animation frame aliases: {changed} created, {verified} already verified")


if __name__ == "__main__":
    main()
