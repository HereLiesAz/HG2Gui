#!/usr/bin/env python3
"""Reproducible per-clip boundary checklist for Guide animations.

Run after organize_guide_material.py (or after finalize_animation_guide.py).
--check verifies that the tracked inventory matches the manifest without writing.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "docs/guide/MANIFEST.json"
OUT = ROOT / "docs/guide/ANIMATION_TODO.md"
PRESENT = {"present", "present_duplicate_identical"}


def supplied(boundary: dict | None) -> bool:
    """Inventory claims must be backed by real files, not just status strings."""
    if not boundary or boundary.get("status") not in PRESENT:
        return False
    local = boundary.get("local")
    return bool(local) and (ROOT / local).is_file()


def counts(entries: list[dict]) -> dict[str, int]:
    count = {"animations": len(entries), "clips": 0, "slots": 0, "supplied": 0}
    for item in entries:
        for scene in item["scenes"]:
            count["clips"] += 1
            for role in ("start", "end"):
                count["slots"] += 1
                count["supplied"] += int(supplied(scene["storyboard"].get(role)))
    return count


def boundary_text(boundary: dict | None, group: str) -> str:
    if not boundary:
        return "⬜ not supplied — unresolved"
    expected = boundary.get("expected") or "not supplied"
    if supplied(boundary):
        caution = " (annotated)" if group == "tangents" else ""
        return f"✅ `{expected}`{caution}"
    label = f"`{expected}`" if boundary.get("expected") else expected
    return f"⬜ {label} — {boundary.get('status') or 'unresolved'}"


def render(data: dict) -> str:
    kinds = (
        ("canonical", "Canonical", data.get("canonical_animations", [])),
        ("tangents", "Tangents", data.get("tangent_animations", [])),
        ("legacy", "Legacy", data.get("legacy_animations", [])),
    )
    stats = [(name, counts(entries)) for _, name, entries in kinds]
    total = {key: sum(c[key] for _, c in stats) for key in ("animations", "clips", "slots", "supplied")}
    unique: dict[str, bool] = {}
    for item in data.get("canonical_animations", []):
        for scene in item["scenes"]:
            for role in ("start", "end"):
                boundary = scene["storyboard"].get(role)
                if boundary and boundary.get("expected"):
                    name = boundary["expected"]
                    unique[name] = unique.get(name, False) or supplied(boundary)
    tangent = stats[1][1]
    rows = [
        "# Animation shot inventory and missing boundaries",
        "",
        "Source: `docs/guide/MANIFEST.json`. Generated review of the original cut-sheet declarations, existing source stills and per-clip START/END roles.",
        "",
        f"**Warning:** The {tangent['supplied']} tangent START images are original screenshots with visible `Start:` labels, **not clean production frames**. All {tangent['slots'] - tangent['supplied']} tangent END frames are unverified.",
        "",
        "| Group | Animations | Clips | Boundary positions | Source images | Absent/unresolved |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for group, count in stats:
        rows.append(f"| {group} | {count['animations']} | {count['clips']} | {count['slots']} | {count['supplied']} | {count['slots'] - count['supplied']} |")
    rows += [
        f"| **Total** | **{total['animations']}** | **{total['clips']}** | **{total['slots']}** | **{total['supplied']}** | **{total['slots'] - total['supplied']}** |",
        "",
        f"Canonical unique named frame files: **{sum(unique.values())}/{len(unique)}**. Canonical neighboring cuts share the identical bytes for common boundaries.",
        "",
        "[Individual clip scripts, shot descriptions and prompts](CLIP_PRODUCTION_INDEX.md)",
        "",
        "## Required outstanding images",
        "",
        "Do not mark an item complete without inspecting the actual asset. A file name alone or a numbered tangent screenshot is not proof of an END shot.",
        "",
    ]
    for kind, title, animations in kinds:
        rows += [f"## {title}", ""]
        for item in animations:
            name = item.get("command") or item.get("name") or Path(item["folder"]).name
            missing: list[str] = []
            clip_rows = ["| Clip | START | END |", "| --- | --- | --- |"]
            for scene in item["scenes"]:
                number = f"{int(scene['scene']):02d}"
                boundaries = scene["storyboard"]
                start = boundary_text(boundaries.get("start"), kind)
                end = boundary_text(boundaries.get("end"), kind)
                link = item["folder"].removeprefix("docs/guide/") + f"/clips/{number}/README.md"
                clip_rows.append(f"| [{number}]({link}) | {start} | {end} |")
                for role in ("start", "end"):
                    frame = boundaries.get(role)
                    if not supplied(frame):
                        expected = frame.get("expected") if frame else None
                        missing.append(f"Clip {number} {role.upper()}: {expected or 'no declared image'}")
            rows += [f"### {name} — {'incomplete' if missing else 'both boundaries supplied'}", "", *clip_rows, ""]
            if missing:
                rows += [f"- [ ] {reason}" for reason in missing]
                rows.append("")
    return "\n".join(rows).rstrip() + "\n"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if checked-in checklist is stale")
    args = parser.parse_args()
    output = render(json.loads(MANIFEST.read_text(encoding="utf-8")))
    if args.check:
        if not OUT.exists() or OUT.read_text(encoding="utf-8") != output:
            raise SystemExit("Animation TODO differs from MANIFEST: regenerate with python3 scripts/build_animation_todo.py")
        print("Animation checklist is current")
    else:
        OUT.write_text(output, encoding="utf-8")
        print(f"Updated {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
