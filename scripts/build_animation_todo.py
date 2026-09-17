#!/usr/bin/env python3
from __future__ import annotations

import json
from collections import OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "docs/guide/MANIFEST.json"
OUT = ROOT / "docs/guide/ANIMATION_TODO.md"
MATERIALIZED = {"present", "present_duplicate_identical"}


def is_materialized(boundary: dict | None) -> bool:
    return bool(boundary) and boundary.get("status") in MATERIALIZED and bool(boundary.get("local"))


def boundary_label(boundary: dict | None) -> str:
    if not boundary:
        return "MISSING RECORD"
    expected = boundary.get("expected") or "not declared"
    if is_materialized(boundary):
        return f"✅ `{expected}`"
    status = boundary.get("status", "unknown")
    return f"⬜ `{expected}` — {status}"


def entry_name(entry: dict, kind: str) -> str:
    if kind == "canonical":
        return f"{entry['index']:02d} — `{entry['command']}`"
    if kind == "legacy":
        return f"legacy — `{entry.get('name', Path(entry['folder']).name)}`"
    return Path(entry["folder"]).name


def collect_counts(entries: list[dict]) -> dict[str, int]:
    counts = {"animations": len(entries), "clips": 0, "slots": 0, "materialized": 0, "missing": 0}
    for entry in entries:
        scenes = entry.get("scenes", [])
        counts["clips"] += len(scenes)
        for scene in scenes:
            storyboard = scene.get("storyboard", {})
            for role in ("start", "end"):
                counts["slots"] += 1
                if is_materialized(storyboard.get(role)):
                    counts["materialized"] += 1
                else:
                    counts["missing"] += 1
    return counts


def unique_boundary_status(entry: dict) -> OrderedDict[str, bool]:
    unique: OrderedDict[str, bool] = OrderedDict()
    for scene in entry.get("scenes", []):
        for role in ("start", "end"):
            boundary = scene.get("storyboard", {}).get(role)
            if not boundary:
                continue
            expected = boundary.get("expected")
            if not expected:
                continue
            unique[expected] = unique.get(expected, False) or is_materialized(boundary)
    return unique


def render_section(entries: list[dict], kind: str) -> list[str]:
    lines: list[str] = []
    title = {"canonical": "Canonical animations", "tangent": "Tangent animations", "legacy": "Legacy / incomplete animations"}[kind]
    lines += [f"## {title}", ""]

    for entry in entries:
        scenes = entry.get("scenes", [])
        complete = all(
            is_materialized(scene.get("storyboard", {}).get(role))
            for scene in scenes
            for role in ("start", "end")
        )
        mark = "x" if complete else " "
        lines += [f"### [{mark}] {entry_name(entry, kind)}", ""]

        if kind == "canonical":
            unique = unique_boundary_status(entry)
            present_unique = sum(unique.values())
            lines.append(
                f"Unique canonical boundary frames: **{present_unique}/{len(unique)} present**. "
                "Adjacent clips intentionally share the same boundary image."
            )
            lines.append("")

        lines += ["| Clip / scene | Start frame | End frame |", "| ---: | --- | --- |"]
        for scene in scenes:
            sb = scene.get("storyboard", {})
            lines.append(
                f"| {int(scene['scene']):02d} | {boundary_label(sb.get('start'))} | {boundary_label(sb.get('end'))} |"
            )
        lines.append("")

        missing_unique: list[str] = []
        if kind == "canonical":
            missing_unique = [name for name, present in unique_boundary_status(entry).items() if not present]
        else:
            for scene in scenes:
                for role in ("start", "end"):
                    boundary = scene.get("storyboard", {}).get(role)
                    if not is_materialized(boundary):
                        expected = boundary.get("expected") if boundary else None
                        label = expected or f"scene {int(scene['scene']):02d} {role} boundary"
                        if label not in missing_unique:
                            missing_unique.append(label)

        if missing_unique:
            lines += ["**TODO — missing boundaries**", ""]
            lines += [f"- [ ] `{name}`" if not name.startswith("scene ") else f"- [ ] {name}" for name in missing_unique]
            lines.append("")
        else:
            lines += ["All start/end boundaries for this animation are materialized.", ""]

    return lines


def main() -> None:
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    canonical = data.get("canonical_animations", [])
    tangents = data.get("tangent_animations", [])
    legacy = data.get("legacy_animations", [])

    groups = [
        ("Canonical", canonical),
        ("Tangents", tangents),
        ("Legacy", legacy),
    ]
    totals = {"animations": 0, "clips": 0, "slots": 0, "materialized": 0, "missing": 0}
    group_counts: list[tuple[str, dict[str, int]]] = []
    for label, entries in groups:
        counts = collect_counts(entries)
        group_counts.append((label, counts))
        for key in totals:
            totals[key] += counts[key]

    canonical_unique = OrderedDict()
    for entry in canonical:
        for name, present in unique_boundary_status(entry).items():
            canonical_unique[name] = canonical_unique.get(name, False) or present

    lines = [
        "# HG2Gui animation TODO",
        "",
        "This file is generated from `docs/guide/MANIFEST.json` by `scripts/build_animation_todo.py`.",
        "It audits every animation clip/scene for both a START and END storyboard boundary.",
        "A checked animation means every start/end boundary used by every clip is backed by an actual image file in the organized Guide tree.",
        "",
        "## Current coverage",
        "",
        "| Group | Animations | Clips / scenes | Boundary slots | Materialized | Missing / unresolved |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for label, counts in group_counts:
        lines.append(
            f"| {label} | {counts['animations']} | {counts['clips']} | {counts['slots']} | {counts['materialized']} | {counts['missing']} |"
        )
    lines.append(
        f"| **Total** | **{totals['animations']}** | **{totals['clips']}** | **{totals['slots']}** | **{totals['materialized']}** | **{totals['missing']}** |"
    )
    lines += [
        "",
        f"Canonical production uses **{len(canonical_unique)} unique boundary image filenames** across the canonical clips because adjacent clips share boundaries. "
        f"**{sum(canonical_unique.values())}/{len(canonical_unique)} unique canonical frames are currently present.**",
        "",
        "## Boundary rules",
        "",
        "- Every clip must have both a START and END image.",
        "- When clip N END is clip N+1 START, it is the **same image**, copied into both scene folders by the organizer.",
        "- A declared filename without an image is not considered complete.",
        "- Missing frames are listed below; no substitute artwork is fabricated.",
        "- Existing source artwork may be canonicalized through `docs/animation_frame_aliases.json`; run `scripts/materialize_animation_frame_aliases.py`, then `scripts/organize_guide_material.py`, then this script.",
        "",
    ]

    lines += render_section(canonical, "canonical")
    lines += render_section(tangents, "tangent")
    lines += render_section(legacy, "legacy")

    OUT.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
    print(f"Wrote {OUT.relative_to(ROOT)}")
    print(
        f"coverage: {totals['materialized']}/{totals['slots']} boundary slots materialized; "
        f"{totals['missing']} missing/unresolved"
    )


if __name__ == "__main__":
    main()
