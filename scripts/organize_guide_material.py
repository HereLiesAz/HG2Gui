#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
OUT = DOCS / "guide"
PACKETS = DOCS / "HG2Gui_animation_5s_production_packets"
CUT_SHEET = DOCS / "HG2Gui_animation_5s_cut_sheet_v2.md"

TANGENTS = [
    ("23_cat_theological_mechanical_history", DOCS / "script (1).md", "cat"),
    ("34_mv_elevator_incident", DOCS / "script (2).md", "elevator"),
    ("02_pkg_install_small_empire_of_dependencies", DOCS / "script (3).md", "package"),
    ("22_find_cartographical_disaster", DOCS / "script.md", "cartography"),
]

IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".webp", ".gif"}
PROJECT_FILES = [
    DOCS / "GUIDE.md",
    DOCS / "Hitchhikers_Guide_to_Termux.md",
    DOCS / "Hitchhikers_Guide_to_Termux.revised_master.editorial_pass_4.md",
    DOCS / "HG2Gui_Animation_Analysis.md",
    DOCS / "HG2Gui_Animation_Assets_Analysis.docx",
    DOCS / "HG2Gui_Animation_Assets_Analysis.md",
    DOCS / "HG2Gui_Animation_Assets_Report.md",
    DOCS / "HG2Gui_Animation_Style_Analysis.md",
    DOCS / "HG2Gui_Animation_Style_Analysis.pdf",
    DOCS / "HG2Gui_animation_5s_cut_sheet.md",
    DOCS / "HG2Gui_animation_5s_cut_sheet_v2.md",
    DOCS / "HG2Gui_animation_6s_prompts.md",
    DOCS / "HG2Gui_animation_6s_prompts_from_5s.md",
    DOCS / "HG2Gui_animation_prompt_pack.md",
    DOCS / "HG2Gui_motion_reference.md",
    DOCS / "guide-tangents.md",
    DOCS / "tangent-scripts.md",
]

CODE_REFERENCES = [
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/guide/GuideContent.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/components/GuideSpacePill.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/components/GuideScreen.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/components/GuideTab.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/components/guide/GuideArticleHeader.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/dialogs/GuideDetailDialog.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/shell/GuideEntryDialog.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/shell/GuideEntryRepository.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/shell/GuideEntryTypes.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/shell/GuideHomeAnimation.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/viewer/GuideArticleParser.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/viewer/GuideEntryRenderer.kt",
    ROOT / "shared/src/commonMain/kotlin/com/hereliesaz/hg2gui/ui/viewer/GuideViewer.kt",
    ROOT / "shared/src/commonTest/kotlin/com/hereliesaz/hg2gui/ui/viewer/GuideArticleParserTest.kt",
]

TEXT_REFERENCE_ROOTS = [
    DOCS / "animation_prompts",
    DOCS / "animation_text",
    DOCS / "animation_texts",
    DOCS / "man_pages",
    DOCS / "woes",
]


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text.rstrip() + "\n", encoding="utf-8")


def copy(src: Path, dst: Path) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)


def rel(path: Path) -> str:
    return str(path.relative_to(ROOT))


def slug(s: str) -> str:
    s = s.replace("`", "").replace("|", " pipe ")
    s = re.sub(r"[^A-Za-z0-9]+", "_", s).strip("_").lower()
    return s or "unnamed"


def parse_cut_sections(block: str) -> dict[int, str]:
    cuts: dict[int, str] = {}
    matches = list(re.finditer(r"(?m)^### Cut (\d{2})[^\n]*\n", block))
    for i, match in enumerate(matches):
        number = int(match.group(1))
        end = matches[i + 1].start() if i + 1 < len(matches) else len(block)
        cuts[number] = block[match.start():end].rstrip()
    return cuts


def parse_packet(packet_text: str) -> dict[int, str]:
    cuts: dict[int, str] = {}
    matches = list(re.finditer(r"(?m)^## Cut (\d{2})[^\n]*\n", packet_text))
    for i, match in enumerate(matches):
        number = int(match.group(1))
        end = matches[i + 1].start() if i + 1 < len(matches) else len(packet_text)
        cuts[number] = packet_text[match.start():end].rstrip()
    return cuts


def copy_reference_set(src_dir: Path, dst_dir: Path) -> list[str]:
    if not src_dir.exists():
        return []
    copied: list[str] = []
    for path in sorted(src_dir.rglob("*")):
        if path.is_file():
            target = dst_dir / path.relative_to(src_dir)
            copy(path, target)
            copied.append(rel(path))
    return copied


def reference_assets() -> list[Path]:
    assets: list[Path] = []
    for path in sorted(DOCS.iterdir()):
        if not path.is_file() or path.suffix.lower() not in IMAGE_EXTS:
            continue
        stem = path.stem.lower()
        if re.match(r"^(cat|elevator|package|cartography)_\d{2}$", stem):
            continue
        if re.match(r"^(echo|ls|ls_tangent)_\d{2}$", stem):
            continue
        assets.append(path)
    return assets


def source_asset_index() -> dict[str, list[Path]]:
    index: dict[str, list[Path]] = {}
    for path in sorted(DOCS.rglob("*")):
        if not path.is_file() or path.suffix.lower() not in IMAGE_EXTS:
            continue
        if OUT in path.parents:
            continue
        index.setdefault(path.name.lower(), []).append(path)
    return index


def file_digest(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def resolve_asset(name: str, asset_index: dict[str, list[Path]]) -> dict:
    candidates = asset_index.get(Path(name).name.lower(), [])
    if not candidates:
        return {
            "expected": name,
            "status": "missing",
            "sources": [],
            "source": None,
        }

    if len(candidates) == 1:
        return {
            "expected": name,
            "status": "present",
            "sources": [rel(candidates[0])],
            "source": candidates[0],
        }

    digests = {file_digest(path) for path in candidates}
    if len(digests) == 1:
        return {
            "expected": name,
            "status": "present_duplicate_identical",
            "sources": [rel(path) for path in candidates],
            "source": candidates[0],
        }

    return {
        "expected": name,
        "status": "ambiguous",
        "sources": [rel(path) for path in candidates],
        "source": None,
    }


def boundary_names(cut_text: str) -> tuple[str | None, str | None]:
    start = re.search(
        r"\*\*START:\*\*\s*`([^`]+\.(?:png|jpg|jpeg|webp|gif))`",
        cut_text,
        re.I,
    )
    end = re.search(
        r"\*\*END:\*\*\s*`([^`]+\.(?:png|jpg|jpeg|webp|gif))`",
        cut_text,
        re.I,
    )
    return (
        start.group(1) if start else None,
        end.group(1) if end else None,
    )


def cut_heading(cut_text: str) -> str:
    match = re.search(r"(?m)^#{2,3} Cut \d{2}\s*—\s*([^\n]+)", cut_text)
    return match.group(1).strip() if match else ""


def cut_summary(cut_text: str) -> str:
    cleaned = re.sub(r"(?m)^#{2,3} Cut [^\n]+\n", "", cut_text, count=1)
    cleaned = re.sub(r"(?m)^\*\*(?:START|END):\*\*.*\n?", "", cleaned)
    cleaned = re.sub(r"(?ms)^### Ready-to-paste generation prompt.*$", "", cleaned)
    paragraphs = [p.strip() for p in re.split(r"\n\s*\n", cleaned) if p.strip()]
    for paragraph in paragraphs:
        if not paragraph.startswith("#"):
            return re.sub(r"\s+", " ", paragraph)
    return ""


def materialize_boundary(
    frame_name: str | None,
    role: str,
    scene_dir: Path,
    asset_index: dict[str, list[Path]],
    semantics: str,
) -> dict:
    if not frame_name:
        return {
            "expected": None,
            "role": role,
            "status": "unresolved",
            "semantics": semantics,
            "sources": [],
            "local": None,
        }

    resolved = resolve_asset(frame_name, asset_index)
    local = None
    source = resolved.pop("source")
    if source is not None:
        local_path = scene_dir / "frames" / f"{role}{source.suffix.lower()}"
        copy(source, local_path)
        local = rel(local_path)

    return {
        **resolved,
        "role": role,
        "semantics": semantics,
        "local": local,
    }


def write_frames_readme(scene_dir: Path, boundaries: list[dict], note: str | None = None) -> None:
    lines = [
        "# Storyboard frames",
        "",
        "Storyboard boundary assets for this scene. These are separate from supporting visual references.",
        "",
    ]
    if note:
        lines += [note, ""]

    for boundary in boundaries:
        role = boundary["role"].capitalize()
        lines.append(f"## {role}")
        lines.append("")
        if boundary["expected"]:
            lines.append(f"- Expected source filename: `{boundary['expected']}`")
        else:
            lines.append("- Expected source filename: not declared")
        lines.append(f"- Status: **{boundary['status']}**")
        lines.append(f"- Semantics: `{boundary['semantics']}`")
        if boundary["local"]:
            local_name = Path(boundary["local"]).name
            lines.append(f"- Local normalized copy: `{local_name}`")
        if boundary["sources"]:
            lines.append("- Source candidate(s):")
            for source in boundary["sources"]:
                lines.append(f"  - `{source}`")
        if boundary["status"] in {"missing", "unresolved", "ambiguous"}:
            lines.append("- No substitute image was fabricated.")
        lines.append("")

    write(scene_dir / "frames" / "README.md", "\n".join(lines))


def storyboard_scene_section(scene_no: int, scene_text: str, boundaries: list[dict]) -> str:
    heading = cut_heading(scene_text)
    summary = cut_summary(scene_text)
    title = f"## Scene {scene_no:02d}"
    if heading:
        title += f" — {heading}"
    lines = [title, ""]
    if summary:
        lines += [summary, ""]

    for boundary in boundaries:
        role = boundary["role"].capitalize()
        if boundary["local"]:
            relative_path = Path(boundary["local"])
            scene_root_marker = Path("scenes") / f"{scene_no:02d}" / "frames" / relative_path.name
            lines.append(
                f"**{role}:** `{scene_root_marker.as_posix()}` "
                f"— source `{boundary['expected']}` ({boundary['status']})"
            )
            lines.append("")
            lines.append(
                f"![Scene {scene_no:02d} {boundary['role']}]"
                f"({scene_root_marker.as_posix()})"
            )
        else:
            expected = f"`{boundary['expected']}`" if boundary["expected"] else "no declared filename"
            lines.append(
                f"**{role}:** **{boundary['status'].upper()}** — {expected}"
            )
        lines.append("")
    return "\n".join(lines).rstrip()


def reference_readme(scene_dir: Path, sources: list[str] | None = None) -> None:
    lines = [
        "# Scene references",
        "",
        "Supporting scene-specific reference material. Storyboard start/end frames live in `../frames/`.",
        "",
    ]
    if sources:
        lines += ["## Present", ""] + [f"- `{source}`" for source in sorted(set(sources))]
    else:
        lines.append("No separate scene-specific supporting references were assigned.")
    write(scene_dir / "references" / "README.md", "\n".join(lines))


def build_canonical(manifest: dict, asset_index: dict[str, list[Path]]) -> None:
    manifest_text = (PACKETS / "00_MANIFEST.md").read_text(encoding="utf-8")
    style_text = (PACKETS / "00_STYLE_LOCK.md").read_text(encoding="utf-8")
    cut_text = CUT_SHEET.read_text(encoding="utf-8")

    packet_lines = re.findall(r"- `([^`]+\.md)` —", manifest_text)
    heading_matches = list(
        re.finditer(r"(?m)^## (\d{2}) — `([^`]+)` — ([^\n]+)\n", cut_text)
    )
    cut_blocks: dict[int, dict] = {}
    for i, match in enumerate(heading_matches):
        index = int(match.group(1))
        end = heading_matches[i + 1].start() if i + 1 < len(heading_matches) else len(cut_text)
        cut_blocks[index] = {
            "command": match.group(2),
            "description": match.group(3),
            "text": cut_text[match.start():end].rstrip(),
        }

    entries = []
    for packet_name in packet_lines:
        index = int(packet_name[:2])
        packet_path = PACKETS / packet_name
        packet_text = packet_path.read_text(encoding="utf-8")
        packet_header = re.match(r"# Animation \d+ — `([^`]+)` — ([^\n]+)", packet_text)
        command = packet_header.group(1) if packet_header else cut_blocks[index]["command"]
        anim_dir = OUT / "animations/canonical" / f"{index:02d}_{slug(command)}"

        write(anim_dir / "animation.md", packet_text)
        write(anim_dir / "cut-sheet.md", cut_blocks[index]["text"])
        write(anim_dir / "style-lock.md", style_text)

        packet_cuts = parse_packet(packet_text)
        cut_sheet_cuts = parse_cut_sections(cut_blocks[index]["text"])
        scene_numbers = sorted(set(packet_cuts) | set(cut_sheet_cuts))
        scene_records = []
        storyboard_sections = [
            f"# Storyboard — {index:02d} `{command}`",
            "",
            f"Boundary declarations come from `{rel(CUT_SHEET)}`.",
            "Actual images are included only when a matching repository asset exists.",
            "Adjacent scenes intentionally duplicate shared boundary frames.",
            "",
        ]

        for number in scene_numbers:
            scene_dir = anim_dir / "scenes" / f"{number:02d}"
            parts = []
            if number in packet_cuts:
                parts.append(packet_cuts[number])
            if number in cut_sheet_cuts:
                parts.append("## Cut-sheet lock\n\n" + cut_sheet_cuts[number])
            scene_text = "\n\n---\n\n".join(parts)
            write(scene_dir / "scene.md", scene_text)

            start_name, end_name = boundary_names(cut_sheet_cuts.get(number, ""))
            start = materialize_boundary(
                start_name,
                "start",
                scene_dir,
                asset_index,
                "explicit_cut_sheet_boundary",
            )
            end = materialize_boundary(
                end_name,
                "end",
                scene_dir,
                asset_index,
                "explicit_cut_sheet_boundary",
            )
            boundaries = [start, end]
            write_frames_readme(scene_dir, boundaries)
            reference_readme(scene_dir)

            storyboard_sections.append(
                storyboard_scene_section(number, cut_sheet_cuts.get(number, scene_text), boundaries)
            )
            storyboard_sections.append("")

            scene_records.append({
                "scene": number,
                "text": rel(scene_dir / "scene.md"),
                "frames_readme": rel(scene_dir / "frames/README.md"),
                "storyboard": {
                    "start": start,
                    "end": end,
                },
                "references_present": [],
                "references_missing": [],
            })

        write(anim_dir / "storyboard.md", "\n".join(storyboard_sections))
        entries.append({
            "index": index,
            "command": command,
            "description": cut_blocks[index]["description"],
            "source_packet": rel(packet_path),
            "source_cut_sheet": rel(CUT_SHEET),
            "folder": rel(anim_dir),
            "storyboard": rel(anim_dir / "storyboard.md"),
            "scenes": scene_records,
        })

    manifest["canonical_animations"] = entries


def inferred_sequence_boundary(
    frame_name: str | None,
    role: str,
    scene_dir: Path,
    asset_index: dict[str, list[Path]],
    kind: str,
) -> dict:
    semantics = (
        f"inferred_{kind}_sequence_boundary"
        if frame_name
        else f"unresolved_{kind}_first_scene_start"
    )
    return materialize_boundary(frame_name, role, scene_dir, asset_index, semantics)


def build_tangents(manifest: dict, asset_index: dict[str, list[Path]]) -> None:
    entries = []
    for folder_name, script_path, prefix in TANGENTS:
        text = script_path.read_text(encoding="utf-8")
        anim_dir = OUT / "animations/tangents" / folder_name
        write(anim_dir / "animation.md", text)

        cut_rows = re.findall(r"\|\s*(\d{2})\s*\|\s*`([^`]+)`\s*\|\s*(.*?)\s*\|", text)
        scene_records = []
        storyboard_sections = [
            f"# Storyboard — {folder_name}",
            "",
            f"Source cut map: `{rel(script_path)}`.",
            "The tangent source declares one frame per cut rather than explicit START/END pairs.",
            "Each declared cut-map frame is treated as that cut's end boundary; the previous cut's frame is reused as the next cut's start boundary.",
            "The first scene's start remains unresolved unless a preceding frame is actually declared elsewhere.",
            "",
        ]
        previous_frame: str | None = None

        for cut_no, frame_name, narration in cut_rows:
            number = int(cut_no)
            scene_dir = anim_dir / "scenes" / cut_no
            scene_text = (
                f"# Scene {cut_no}\n\n"
                f"**Frame:** `{frame_name}`\n\n"
                f"**Narration beat:** {narration}"
            )
            write(scene_dir / "scene.md", scene_text)

            start = inferred_sequence_boundary(
                previous_frame,
                "start",
                scene_dir,
                asset_index,
                "tangent_cut_map",
            )
            end = inferred_sequence_boundary(
                frame_name,
                "end",
                scene_dir,
                asset_index,
                "tangent_cut_map",
            )
            boundaries = [start, end]
            write_frames_readme(
                scene_dir,
                boundaries,
                "Boundary roles are inferred from the ordered cut-map still sequence; the source script itself declares one frame per cut.",
            )
            reference_readme(scene_dir)

            storyboard_sections += [
                storyboard_scene_section(
                    number,
                    f"## Cut {cut_no}\n\n{narration}",
                    boundaries,
                ),
                "",
            ]

            scene_records.append({
                "scene": number,
                "frame": frame_name,
                "narration": narration,
                "text": rel(scene_dir / "scene.md"),
                "storyboard": {"start": start, "end": end},
                "references_present": [],
                "references_missing": [],
            })
            previous_frame = frame_name

        write(anim_dir / "storyboard.md", "\n".join(storyboard_sections))
        entries.append({
            "folder": rel(anim_dir),
            "source_script": rel(script_path),
            "frame_prefix": prefix,
            "storyboard": rel(anim_dir / "storyboard.md"),
            "scenes": scene_records,
        })

    manifest["tangent_animations"] = entries


def build_legacy(manifest: dict, asset_index: dict[str, list[Path]]) -> None:
    groups: dict[str, list[tuple[int, Path]]] = {}
    for path in sorted(DOCS.iterdir()):
        if not path.is_file() or path.suffix.lower() not in IMAGE_EXTS:
            continue
        match = re.match(r"^(echo|ls|ls_tangent)_(\d{2})$", path.stem.lower())
        if not match:
            continue
        groups.setdefault(match.group(1), []).append((int(match.group(2)), path))

    legacy = []
    for name, frames in sorted(groups.items()):
        frames = sorted(frames)
        anim_dir = OUT / "animations/legacy" / name
        write(
            anim_dir / "animation.md",
            "\n".join([
                f"# Legacy/incomplete animation — `{name}`",
                "",
                "Reference/storyboard images exist in the repository, but this animation is not represented by a current 5-second production packet.",
                "",
                "The ordered still sequence is preserved here without promoting it to canonical.",
            ]),
        )

        storyboard_sections = [
            f"# Storyboard — legacy `{name}`",
            "",
            "The loose numbered image sequence is interpreted as ordered cut-end stills.",
            "Each previous still is duplicated as the next scene's inferred start frame.",
            "The first scene's start remains unresolved because no earlier boundary is declared.",
            "",
        ]
        scene_records = []
        previous_frame: str | None = None

        for number, source in frames:
            scene_dir = anim_dir / "scenes" / f"{number:02d}"
            write(
                scene_dir / "scene.md",
                f"# Scene {number:02d}\n\nExisting ordered storyboard frame: `{source.name}`.\n",
            )
            start = inferred_sequence_boundary(
                previous_frame,
                "start",
                scene_dir,
                asset_index,
                "legacy_still",
            )
            end = inferred_sequence_boundary(
                source.name,
                "end",
                scene_dir,
                asset_index,
                "legacy_still",
            )
            boundaries = [start, end]
            write_frames_readme(
                scene_dir,
                boundaries,
                "Boundary roles are inferred from the loose numbered still sequence.",
            )
            reference_readme(scene_dir)

            storyboard_sections += [
                storyboard_scene_section(
                    number,
                    f"## Cut {number:02d}\n\nExisting ordered storyboard frame `{source.name}`.",
                    boundaries,
                ),
                "",
            ]
            scene_records.append({
                "scene": number,
                "source_frame": rel(source),
                "text": rel(scene_dir / "scene.md"),
                "storyboard": {"start": start, "end": end},
            })
            previous_frame = source.name

        write(anim_dir / "storyboard.md", "\n".join(storyboard_sections))
        legacy.append({
            "folder": rel(anim_dir),
            "name": name,
            "storyboard": rel(anim_dir / "storyboard.md"),
            "scenes": scene_records,
        })

    manifest["legacy_animations"] = legacy


def build_project(manifest: dict) -> None:
    project = OUT / "project"
    copied: list[str] = []
    for src in PROJECT_FILES:
        if src.exists():
            copy(src, project / "source" / src.name)
            copied.append(rel(src))
    for src in CODE_REFERENCES:
        if src.exists():
            copy(src, project / "code" / src.relative_to(ROOT))
            copied.append(rel(src))
    for src_root in TEXT_REFERENCE_ROOTS:
        copied.extend(copy_reference_set(src_root, project / "reference-text" / src_root.name))
    shared = reference_assets()
    for src in shared:
        copy(src, project / "references/shared" / src.name)
        copied.append(rel(src))
    manifest["project_files"] = sorted(copied)


def build_unassigned(manifest: dict) -> None:
    used_names: set[str] = set()

    for entry in manifest.get("tangent_animations", []):
        for scene in entry["scenes"]:
            for boundary in scene["storyboard"].values():
                used_names.update(Path(source).name for source in boundary["sources"])

    for entry in manifest.get("legacy_animations", []):
        for scene in entry["scenes"]:
            for boundary in scene["storyboard"].values():
                used_names.update(Path(source).name for source in boundary["sources"])

    unassigned: list[str] = []
    for path in sorted(DOCS.iterdir()):
        if not path.is_file() or path.suffix.lower() not in IMAGE_EXTS:
            continue
        stem = path.stem.lower()
        if re.match(r"^(cat|elevator|package|cartography|echo|ls|ls_tangent)_\d{2}$", stem):
            if path.name not in used_names:
                copy(path, OUT / "project/references/unassigned" / path.name)
                unassigned.append(rel(path))
    manifest["unassigned_reference_media"] = unassigned


def coverage_for(entries: list[dict]) -> dict:
    counts = {
        "scenes": 0,
        "boundary_slots": 0,
        "present": 0,
        "present_duplicate_identical": 0,
        "missing": 0,
        "ambiguous": 0,
        "unresolved": 0,
    }
    for entry in entries:
        for scene in entry["scenes"]:
            counts["scenes"] += 1
            storyboard = scene.get("storyboard", {})
            for role in ("start", "end"):
                boundary = storyboard.get(role)
                if not boundary:
                    continue
                counts["boundary_slots"] += 1
                status = boundary["status"]
                if status in counts:
                    counts[status] += 1
    counts["materialized"] = counts["present"] + counts["present_duplicate_identical"]
    counts["not_materialized"] = (
        counts["missing"] + counts["ambiguous"] + counts["unresolved"]
    )
    return counts


def main() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)

    asset_index = source_asset_index()
    manifest: dict = {
        "generated_from_repository": True,
        "note": (
            "Non-destructive organization. Original source files remain in place; "
            "copies are organized under docs/guide. Missing storyboard frames are "
            "reported, never fabricated."
        ),
    }

    build_canonical(manifest, asset_index)
    build_tangents(manifest, asset_index)
    build_legacy(manifest, asset_index)
    build_project(manifest)
    build_unassigned(manifest)

    canonical_scene_count = sum(len(entry["scenes"]) for entry in manifest["canonical_animations"])
    tangent_scene_count = sum(len(entry["scenes"]) for entry in manifest["tangent_animations"])
    legacy_scene_count = sum(len(entry["scenes"]) for entry in manifest["legacy_animations"])

    coverage = {
        "canonical": coverage_for(manifest["canonical_animations"]),
        "tangents": coverage_for(manifest["tangent_animations"]),
        "legacy": coverage_for(manifest["legacy_animations"]),
    }
    manifest["storyboard_coverage"] = coverage

    readme = f"""# HG2Gui Guide material

This is the organized, non-destructive collection of Guide-related production material. Source files remain in their original locations so application and documentation paths do not break.

## Structure

- `project/` — Guide-wide prose, design/motion references, shared production rules, relevant source-code copies, and unresolved references.
- `animations/canonical/` — the 35 current technical 5-second production-packet entries.
- `animations/tangents/` — the four scripted Guide tangents that precede canonical entries.
- `animations/legacy/` — older/incomplete animated material that has numbered visual material but no current production packet.

Every animation folder contains its complete animation text and a `storyboard.md`. Every `scenes/NN/scene.md` contains only material belonging to that scene.

## Visual material

The three visual classes are deliberately separate:

1. `scenes/NN/frames/` — storyboard boundary frames. `start.*` and `end.*` are normalized copies of real repository assets when those assets exist.
2. `scenes/NN/references/` — supporting visual material specific to that scene, excluding storyboard boundaries.
3. `project/references/shared/` — Guide-wide style and design references.

Adjacent scenes may intentionally contain duplicate copies of the same boundary image: one scene's end is the next scene's start. Duplication here is structural, not accidental.

Canonical START/END filenames come directly from `{CUT_SHEET.name}`. If a declared image does not actually exist in the repository, its `frames/README.md`, the animation `storyboard.md`, and `MANIFEST.json` record the absence. No replacement frame is invented.

Tangent and legacy sources generally provide one ordered still per cut rather than explicit START/END pairs. Those stills are treated as cut-end boundaries and the previous still is reused as the following cut's inferred start. The first cut's start remains explicitly unresolved where no preceding still exists.

## Inventory

- Canonical animations: **{len(manifest["canonical_animations"])}**
- Canonical scenes: **{canonical_scene_count}**
- Tangent animations: **{len(manifest["tangent_animations"])}**
- Tangent scenes: **{tangent_scene_count}**
- Legacy/incomplete animation groups: **{len(manifest["legacy_animations"])}**
- Legacy scenes: **{legacy_scene_count}**
- Project-wide files copied: **{len(manifest["project_files"])}**
- Unassigned scene/reference media retained for review: **{len(manifest["unassigned_reference_media"])}**

## Storyboard boundary coverage

- Canonical: **{coverage["canonical"]["materialized"]} / {coverage["canonical"]["boundary_slots"]}** boundary slots materialized; **{coverage["canonical"]["missing"]}** missing; **{coverage["canonical"]["ambiguous"]}** ambiguous; **{coverage["canonical"]["unresolved"]}** unresolved.
- Tangents: **{coverage["tangents"]["materialized"]} / {coverage["tangents"]["boundary_slots"]}** boundary slots materialized; **{coverage["tangents"]["missing"]}** missing; **{coverage["tangents"]["ambiguous"]}** ambiguous; **{coverage["tangents"]["unresolved"]}** unresolved.
- Legacy: **{coverage["legacy"]["materialized"]} / {coverage["legacy"]["boundary_slots"]}** boundary slots materialized; **{coverage["legacy"]["missing"]}** missing; **{coverage["legacy"]["ambiguous"]}** ambiguous; **{coverage["legacy"]["unresolved"]}** unresolved.

`MANIFEST.json` records provenance, source semantics, and exact storyboard coverage.
"""
    write(OUT / "README.md", readme)
    write(OUT / "MANIFEST.json", json.dumps(manifest, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
