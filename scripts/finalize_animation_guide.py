#!/usr/bin/env python3
from __future__ import annotations

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
GUIDE = DOCS / "guide"
MANIFEST = GUIDE / "MANIFEST.json"

# Loose source artwork that belongs to a particular canonical animation rather
# than the Guide-wide reference pool. Source files remain in place; organized
# copies live with the animation.
CANONICAL_REFERENCES: dict[str, list[tuple[str, str]]] = {
    "01_pwd": [
        ("command_line_protest_at_the_union_house.png", "sequence artwork / alternate pwd composition"),
        ("directory_protesters_and_terminal_prompt.png", "sequence artwork / pwd picket-line composition"),
        ("imagegen.png", "sequence artwork / pwd worker-huddle composition"),
        ("mustard_design_system_character_sheet.png", "character and prop reference sheet"),
        ("tech_protest_workers_demand_change.png", "sequence artwork / cd .. protest composition"),
        ("workers_rise_with_a_command_prompt.png", "sequence artwork / directory-transition composition"),
    ],
    "02_pkg_install": [
        ("ChatGPT Image Aug 14, 2026, 12_01_47 AM.png", "source artwork canonicalized as frame_00"),
        ("ChatGPT Image Aug 14, 2026, 12_01_40 AM.png", "source artwork canonicalized as frame_01"),
        ("ChatGPT Image Aug 14, 2026, 12_01_16 AM.png", "source artwork canonicalized as frame_03"),
        ("ChatGPT Image Aug 14, 2026, 12_00_50 AM.png", "source artwork canonicalized as frame_04"),
        ("02_pkg_install_frame_00.png", "canonical boundary frame_00"),
        ("02_pkg_install_frame_01.png", "canonical boundary frame_01"),
        ("02_pkg_install_frame_03.png", "canonical boundary frame_03"),
        ("02_pkg_install_frame_04.png", "canonical boundary frame_04"),
    ],
    "03_apt_get_update": [
        ("bf8c1062-ae0c-4f76-8f39-d1295c9a851d.png", "source artwork canonicalized as frame_01"),
        ("03_apt_get_update_frame_01.png", "canonical boundary frame_01"),
    ],
}

TANGENT_SOURCE_FRAMES = {
    "02_pkg_install_small_empire_of_dependencies": "package",
    "22_find_cartographical_disaster": "cartography",
    "23_cat_theological_mechanical_history": "cat",
    "34_mv_elevator_incident": "elevator",
}

LEGACY_SOURCE_FRAMES = {
    "echo": "echo_clip_",
    "ls": "ls_clip_",
    "ls_tangent": "ls_tangent_clip_",
}


def copy_file(src: Path, dst: Path) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)


def remove_from_shared(name: str) -> None:
    path = GUIDE / "project/references/shared" / name
    if path.exists():
        path.unlink()


def write_reference_readme(anim_dir: Path, rows: list[tuple[str, str, str]]) -> None:
    lines = [
        "# Animation references",
        "",
        "Animation-specific source artwork and production references collected here so this animation is self-contained under `docs/guide/`.",
        "Original source files remain in their original repository locations; these are organized copies.",
        "Storyboard boundary files used by individual clips live in each scene's `frames/` directory.",
        "",
        "| Organized file | Original source | Role |",
        "| --- | --- | --- |",
    ]
    for organized, source, role in rows:
        lines.append(f"| `{organized}` | `{source}` | {role} |")
    (anim_dir / "references").mkdir(parents=True, exist_ok=True)
    (anim_dir / "references/README.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def organize_canonical(manifest: dict) -> dict[str, list[dict]]:
    by_folder = {Path(entry["folder"]).name: entry for entry in manifest["canonical_animations"]}
    report: dict[str, list[dict]] = {}
    for folder, items in CANONICAL_REFERENCES.items():
        entry = by_folder.get(folder)
        if entry is None:
            raise RuntimeError(f"Canonical animation folder missing from manifest: {folder}")
        anim_dir = ROOT / entry["folder"]
        rows: list[tuple[str, str, str]] = []
        records: list[dict] = []
        for name, role in items:
            src = DOCS / name
            if not src.exists():
                raise RuntimeError(f"Expected animation source asset missing: {src}")
            dst = anim_dir / "references/source" / name
            copy_file(src, dst)
            remove_from_shared(name)
            organized = dst.relative_to(anim_dir).as_posix()
            source = src.relative_to(ROOT).as_posix()
            rows.append((organized, source, role))
            records.append({"source": source, "organized": dst.relative_to(ROOT).as_posix(), "role": role})
        write_reference_readme(anim_dir, rows)
        entry["animation_references"] = records
        report[folder] = records
    return report


def organize_tangents(manifest: dict) -> dict[str, list[dict]]:
    by_folder = {Path(entry["folder"]).name: entry for entry in manifest["tangent_animations"]}
    report: dict[str, list[dict]] = {}
    for folder, prefix in TANGENT_SOURCE_FRAMES.items():
        entry = by_folder.get(folder)
        if entry is None:
            raise RuntimeError(f"Tangent animation folder missing from manifest: {folder}")
        anim_dir = ROOT / entry["folder"]
        sources = sorted(DOCS.glob(f"{prefix}_[0-9][0-9].png"))
        rows: list[tuple[str, str, str]] = []
        records: list[dict] = []
        for src in sources:
            dst = anim_dir / "references/source-frames" / src.name
            copy_file(src, dst)
            organized = dst.relative_to(anim_dir).as_posix()
            source = src.relative_to(ROOT).as_posix()
            role = "ordered tangent source frame"
            rows.append((organized, source, role))
            records.append({"source": source, "organized": dst.relative_to(ROOT).as_posix(), "role": role})
        write_reference_readme(anim_dir, rows)
        entry["animation_references"] = records
        report[folder] = records
    return report


def organize_legacy(manifest: dict) -> dict[str, list[dict]]:
    by_folder = {Path(entry["folder"]).name: entry for entry in manifest["legacy_animations"]}
    report: dict[str, list[dict]] = {}
    for folder, prefix in LEGACY_SOURCE_FRAMES.items():
        entry = by_folder.get(folder)
        if entry is None:
            continue
        anim_dir = ROOT / entry["folder"]
        sources = sorted(DOCS.glob(f"{prefix}[0-9][0-9]_*.png"))
        rows: list[tuple[str, str, str]] = []
        records: list[dict] = []
        for src in sources:
            dst = anim_dir / "references/source-frames" / src.name
            copy_file(src, dst)
            organized = dst.relative_to(anim_dir).as_posix()
            source = src.relative_to(ROOT).as_posix()
            role = "explicit legacy clip boundary source"
            rows.append((organized, source, role))
            records.append({"source": source, "organized": dst.relative_to(ROOT).as_posix(), "role": role})
        write_reference_readme(anim_dir, rows)
        entry["animation_references"] = records
        report[folder] = records
    return report


def verify_boundaries(manifest: dict) -> dict:
    failures: list[str] = []
    counts = {"animations": 0, "scenes": 0, "boundary_slots": 0, "materialized": 0, "missing_or_unresolved": 0}
    groups = ["canonical_animations", "tangent_animations", "legacy_animations"]
    materialized_statuses = {"present", "present_duplicate_identical"}

    for group in groups:
        for entry in manifest.get(group, []):
            counts["animations"] += 1
            for scene in entry.get("scenes", []):
                counts["scenes"] += 1
                scene_dir = ROOT / scene["text"].rsplit("/", 1)[0]
                frames_readme = scene_dir / "frames/README.md"
                if not frames_readme.exists():
                    failures.append(f"{scene_dir.relative_to(ROOT)}: missing frames/README.md")
                board = scene.get("storyboard", {})
                for role in ("start", "end"):
                    counts["boundary_slots"] += 1
                    boundary = board.get(role)
                    if boundary is None:
                        failures.append(f"{scene_dir.relative_to(ROOT)}: missing {role} storyboard record")
                        continue
                    status = boundary.get("status", "unresolved")
                    local = boundary.get("local")
                    if status in materialized_statuses:
                        counts["materialized"] += 1
                        if not local or not (ROOT / local).exists():
                            failures.append(f"{scene_dir.relative_to(ROOT)}: {role} marked {status} but local frame is absent")
                    else:
                        counts["missing_or_unresolved"] += 1

    if failures:
        raise RuntimeError("Boundary verification failed:\n" + "\n".join(failures))
    return counts


def build_content_index(manifest: dict, reference_report: dict, verification: dict) -> None:
    lines = [
        "# Animation content index",
        "",
        "All Guide animation production material is organized under `docs/guide/animations/`.",
        "Original source files remain where they were so existing repository links do not break.",
        "",
        "## Boundary audit",
        "",
        f"- Animations: **{verification['animations']}**",
        f"- Clips / scenes: **{verification['scenes']}**",
        f"- START/END boundary slots: **{verification['boundary_slots']}**",
        f"- Boundary slots backed by real organized image files: **{verification['materialized']}**",
        f"- Missing or unresolved boundary slots: **{verification['missing_or_unresolved']}**",
        "- Detailed actionable list: [`ANIMATION_TODO.md`](ANIMATION_TODO.md)",
        "",
        "## Organization",
        "",
        "Each animation directory contains its complete animation text, storyboard/cut information, scene folders, and animation-specific references when available.",
        "Each scene has a `frames/` directory with explicit START and END records. Real boundary images are normalized to `start.*` and `end.*`; missing artwork is recorded rather than fabricated.",
        "",
        "### Animation-specific loose media moved out of the shared bucket",
        "",
    ]
    for folder in sorted(reference_report):
        lines.append(f"- `{folder}` — {len(reference_report[folder])} source asset(s) organized with the animation")
    lines += [
        "",
        "Guide-wide film/style screenshots, logos, and banners remain under `project/` because they are not specific to one animation.",
        "",
    ]
    (GUIDE / "ANIMATION_CONTENT_INDEX.md").write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    if not MANIFEST.exists():
        raise RuntimeError("Run scripts/organize_guide_material.py before this script")
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))

    canonical = organize_canonical(manifest)
    tangents = organize_tangents(manifest)
    legacy = organize_legacy(manifest)
    verification = verify_boundaries(manifest)

    manifest["animation_reference_organization"] = {
        "canonical": canonical,
        "tangents": tangents,
        "legacy": legacy,
        "note": "Animation-specific loose media is copied into the owning animation folder; repository source files remain untouched.",
    }
    manifest["boundary_verification"] = verification
    MANIFEST.write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    combined: dict[str, list[dict]] = {}
    combined.update(canonical)
    combined.update(tangents)
    combined.update(legacy)
    build_content_index(manifest, combined, verification)
    print(json.dumps({"boundary_verification": verification, "organized_animation_reference_groups": {k: len(v) for k, v in combined.items()}}, indent=2))


if __name__ == "__main__":
    main()
