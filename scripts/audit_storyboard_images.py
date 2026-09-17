#!/usr/bin/env python3
"""Inventory source image dimensions while locating legacy storyboard sheets."""

from pathlib import Path
from PIL import Image
import json

EXTS = {".png", ".jpg", ".jpeg", ".webp"}
rows = []
for path in sorted(Path("docs").rglob("*")):
    if not path.is_file() or path.suffix.lower() not in EXTS:
        continue
    if path.as_posix().startswith("docs/guide/"):
        continue
    try:
        with Image.open(path) as image:
            width, height = image.size
            rows.append(
                {
                    "path": path.as_posix(),
                    "width": width,
                    "height": height,
                    "ratio": round(width / height, 4),
                    "bytes": path.stat().st_size,
                }
            )
    except Exception as exc:
        rows.append({"path": path.as_posix(), "error": str(exc)})

print("STORYBOARD_IMAGE_AUDIT_BEGIN")
print(json.dumps(rows, indent=2))
print("STORYBOARD_IMAGE_AUDIT_END")
