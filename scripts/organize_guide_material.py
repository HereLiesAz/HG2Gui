#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import shutil
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
OUT = DOCS / "guide"

TEXT_SUFFIXES = {".md", ".txt", ".html"}
MEDIA_SUFFIXES = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg", ".mp4", ".mov", ".webm"}


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text.rstrip() + "\n", encoding="utf-8")


def copy_file(src: Path, dst: Path) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)


def safe_name(value: str) -> str:
    value = value.strip().lower().replace("`", "")
    value = re.sub(r"[^a-z0-9]+", "_", value)
    return value.strip("_")


def parse_cut_blocks(text: str, heading_level: int = 2) -> dict[str, str]:
    hashes = "#" * heading_level
    rx = re.compile(
        rf"(?ms)^{re.escape(hashes)} Cut (\d{{2}}) — ([^\n]+)\n(.*?)(?=^{re.escape(hashes)} Cut \d{{2}} — |\Z)"
    )
    blocks: dict[str, str] = {}
    for match in rx.finditer(text):
        cut = match.group(1)
        blocks[cut] = f"{hashes} Cut {cut} — {match.group(2)}\n{match.group(3)}".strip() + "\n"
    return blocks


def parse_cut_sheet(text: str) -> tuple[dict[str, str], dict[str, dict[str, str]]]:
    sections: dict[str, str] = {}
    cuts: dict[str, dict[str, str]] = {}
    rx = re.compile(r"(?ms)^## (\d{2}) — (.*?)(?=^## \d{2} — |\Z)")
    for match in rx.finditer(text):
        number = match.group(1)
        section = f"## {number} — {match.group(2)}".strip() + "\n"
        sections[number] = section
        cuts[number] = parse_cut_blocks(section, heading_level=3)
    return sections, cuts


def parse_tangent_cut_map(text: str) -> list[tuple[str, str, str]]:
    rows: list[tuple[str, str, str]] = []
    rx = re.compile(r"(?m)^\|\s*(\d{2})\s*\|\s*`([^`]+)`\s*\|\s*(.*?)\s*\|\s*$")
    for cut, frame, narration in rx.findall(text):
        rows.append((cut, frame.strip(), narration.strip()))
    return rows


def markdown_title(text: str, fallback: str) -> str:
    match = re.search(r"(?m)^#\s+(.+?)\s*$", text)
    return match.group(1).strip() if match else fallback


def command_sections(command: str, sources: list[Path]) -> str:
    escaped = re.escape(command)
    pieces: list[str] = []
    heading = re.compile(rf"(?ms)^###\s+`?{escaped}`?\s+—[^\n]*\n.*?(?=^###\s+|^##\s+|\Z)", re.IGNORECASE)
    for source in sources:
        if not source.exists():
            continue
        text = read(source)
        match = heading.search(text)
        if match:
            pieces.append(f"## Source: `{source.relative_to(ROOT).as_posix()}`\n\n{match.group(0).strip()}")
    return "\n\n---\n\n".join(pieces)


def is_under(path: Path, parent: Path) -> bool:
    try:
        path.relative_to(parent)
        return True
    except ValueError:
        return False


def build_source_index() -> dict[str, list[Path]]:
    index: dict[str, list[Path]] = defaultdict(list)
    for path in DOCS.rglob("*"):
        if path.is_file() and not is_under(path, OUT):
            index[path.name].append(path)
    return index


def copy_project_material(source_index: dict[str, list[Path]]) -> list[str]:
    copied: list[str] = []
    texts = OUT / "project" / "texts"
    design = OUT / "project" / "design"
    source = OUT / "project" / "source"
    shared_refs = OUT / "project" / "references" / "shared"

    explicit_texts = {
        "GUIDE.md",
        "USER_GUIDE.md",
        "guide-tangents.md",
        "tangent-scripts.md",
        "Hitchhikers_Guide_to_Termux.md",
        "Hitchhikers_Guide_to_Termux.revised_master.editorial_pass_4.md",
        "Hitchhikers_Guide_to_Termux.all_animation_sequences.md",
        "HG2Gui_all_animation_5s_production_packets_v2.md",
        "HG2Gui_animation_5s_cut_sheet_v2.md",
        "HG2Gui_animation_style_lock_prompt_v2.md",
        "HG2Gui_character_consistency_and_frames_plan.md",
    }

    for path in sorted(DOCS.iterdir()):
        if not path.is_file():
            continue
        lower = path.name.lower()
        if path.name in explicit_texts or (
            path.suffix.lower() in TEXT_SUFFIXES
            and ("hitchhiker" in lower or "guide" in lower or "animation" in lower or "tangent" in lower or path.name.startswith("script"))
        ):
            bucket = design if path.suffix.lower() == ".html" else texts
            dst = bucket / path.name
            copy_file(path, dst)
            copied.append(dst.relative_to(ROOT).as_posix())

    source_candidates: list[Path] = []
    for suffix in ("*.kt", "*.java", "*.xml"):
        for path in ROOT.rglob(suffix):
            posix = path.as_posix().lower()
            if any(part in posix for part in ("/.git/", "/build/", "/.gradle/", "/docs/guide/")):
                continue
            if "guide" in path.name.lower() or "/ui/guide/" in posix:
                source_candidates.append(path)

    for path in sorted(set(source_candidates)):
        rel = path.relative_to(ROOT)
        dst = source / rel
        copy_file(path, dst)
        copied.append(dst.relative_to(ROOT).as_posix())

    shared_patterns = (
        re.compile(r"^mustard_design_system_", re.I),
        re.compile(r"protest", re.I),
        re.compile(r"workers?_", re.I),
        re.compile(r"^ChatGPT Image ", re.I),
    )
    for path in sorted(DOCS.iterdir()):
        if not path.is_file() or path.suffix.lower() not in MEDIA_SUFFIXES:
            continue
        if any(rx.search(path.name) for rx in shared_patterns):
            dst = shared_refs / path.name
            copy_file(path, dst)
            copied.append(dst.relative_to(ROOT).as_posix())

    # Production-level source documents that apply across animations.
    production = OUT / "project" / "production"
    packet_dir = DOCS / "HG2Gui_animation_5s_production_packets"
    for name in ("00_MANIFEST.md", "00_STYLE_LOCK.md"):
        path = packet_dir / name
        if path.exists():
            dst = production / name
            copy_file(path, dst)
            copied.append(dst.relative_to(ROOT).as_posix())

    return copied


def organize_canonical(source_index: dict[str, list[Path]], manifest: dict) -> None:
    packet_dir = DOCS / "HG2Gui_animation_5s_production_packets"
    cut_sheet_path = DOCS / "HG2Gui_animation_5s_cut_sheet_v2.md"
    cut_sheet = read(cut_sheet_path) if cut_sheet_path.exists() else ""
    sheet_sections, sheet_cuts = parse_cut_sheet(cut_sheet)
    style_lock = packet_dir / "00_STYLE_LOCK.md"

    for packet in sorted(packet_dir.glob("[0-9][0-9]_*_5s_packet.md")):
        match = re.match(r"^(\d{2})_(.+)_5s_packet\.md$", packet.name)
        if not match:
            continue
        number, slug = match.groups()
        animation_dir = OUT / "animations" / "canonical" / f"{number}_{slug}"
        text = read(packet)
        title = markdown_title(text, packet.stem)
        copy_file(packet, animation_dir / "animation.md")
        if style_lock.exists():
            copy_file(style_lock, animation_dir / "style-lock.md")
        if number in sheet_sections:
            write(animation_dir / "cut-sheet.md", sheet_sections[number])

        packet_cuts = parse_cut_blocks(text, heading_level=2)
        scene_records = []
        for cut in sorted(set(packet_cuts) | set(sheet_cuts.get(number, {}))):
            scene_dir = animation_dir / "scenes" / cut
            parts = []
            if cut in packet_cuts:
                parts.append(packet_cuts[cut].strip())
            if cut in sheet_cuts.get(number, {}):
                parts.append("## Cut-sheet lock\n\n" + sheet_cuts[number][cut].strip())
            write(scene_dir / "scene.md", "\n\n---\n\n".join(parts))

            expected = sorted(set(re.findall(r"`([^`]+\.(?:png|jpg|jpeg|webp|svg))`", sheet_cuts.get(number, {}).get(cut, ""), re.I)))
            found: list[str] = []
            missing: list[str] = []
            for filename in expected:
                candidates = [p for p in source_index.get(filename, []) if not is_under(p, OUT)]
                if candidates:
                    for src in candidates:
                        dst = scene_dir / "references" / src.name
                        copy_file(src, dst)
                        found.append(src.relative_to(ROOT).as_posix())
                else:
                    missing.append(filename)

            ref_lines = ["# Scene reference inventory", ""]
            if found:
                ref_lines += ["## Present", ""] + [f"- `{item}`" for item in found] + [""]
            if missing:
                ref_lines += ["## Planned / not present in repository", ""] + [f"- `{item}`" for item in missing] + [""]
            if not found and not missing:
                ref_lines += ["No scene-specific reference filenames are declared in the cut sheet.", ""]
            write(scene_dir / "references" / "README.md", "\n".join(ref_lines))
            scene_records.append({"scene": cut, "references_found": found, "references_missing": missing})

        manifest["canonical"].append(
            {
                "number": number,
                "slug": slug,
                "title": title,
                "source": packet.relative_to(ROOT).as_posix(),
                "scenes": scene_records,
            }
        )


def organize_tangents(source_index: dict[str, list[Path]], manifest: dict) -> None:
    specs = [
        ("script (3).md", "02_pkg_install_small_empire_of_dependencies", "package"),
        ("script.md", "22_find_cartographical_disaster", "cartography"),
        ("script (1).md", "23_cat_theological_mechanical_history", "cat"),
        ("script (2).md", "34_mv_elevator_incident", "elevator"),
    ]
    for source_name, slug, frame_prefix in specs:
        src = DOCS / source_name
        if not src.exists():
            continue
        text = read(src)
        title = markdown_title(text, slug)
        animation_dir = OUT / "animations" / "tangents" / slug
        copy_file(src, animation_dir / "animation.md")
        rows = parse_tangent_cut_map(text)
        scene_records = []
        for cut, frame, narration in rows:
            scene_dir = animation_dir / "scenes" / cut
            scene_text = (
                f"# Scene {cut}\n\n"
                f"**Animation:** {title}\n\n"
                f"**Frame:** `{frame}`\n\n"
                f"## Narration beat\n\n{narration}\n"
            )
            write(scene_dir / "scene.md", scene_text)
            found: list[str] = []
            candidates = [p for p in source_index.get(frame, []) if not is_under(p, OUT)]
            for ref in candidates:
                dst = scene_dir / "references" / ref.name
                copy_file(ref, dst)
                found.append(ref.relative_to(ROOT).as_posix())
            # Include any alternate scene-specific media using the same tangent prefix and cut number.
            alt_rx = re.compile(rf"^{re.escape(frame_prefix)}[_-]{re.escape(cut)}(?:[_\.-].*)?", re.I)
            for name, paths in source_index.items():
                if not alt_rx.match(name) or name == frame:
                    continue
                for ref in paths:
                    if ref.suffix.lower() in MEDIA_SUFFIXES and not is_under(ref, OUT):
                        dst = scene_dir / "references" / ref.name
                        copy_file(ref, dst)
                        found.append(ref.relative_to(ROOT).as_posix())
            write(
                scene_dir / "references" / "README.md",
                "# Scene reference inventory\n\n"
                + ("\n".join(f"- `{item}`" for item in sorted(set(found))) if found else f"- Missing expected reference: `{frame}`"),
            )
            scene_records.append({"scene": cut, "frame": frame, "references_found": sorted(set(found))})
        manifest["tangents"].append(
            {
                "slug": slug,
                "title": title,
                "source": src.relative_to(ROOT).as_posix(),
                "scenes": scene_records,
            }
        )


def organize_legacy(source_index: dict[str, list[Path]], manifest: dict) -> None:
    groups = {
        "echo": re.compile(r"^echo_clip_(\d{2})_(start|end)\.(png|jpg|jpeg|webp)$", re.I),
        "ls": re.compile(r"^ls_clip_(\d{2})_(start|end)\.(png|jpg|jpeg|webp)$", re.I),
        "ls_tangent": re.compile(r"^ls_tangent_clip_(\d{2})_(start|end)\.(png|jpg|jpeg|webp)$", re.I),
    }
    prose_sources = [
        DOCS / "GUIDE.md",
        DOCS / "Hitchhikers_Guide_to_Termux.md",
        DOCS / "Hitchhikers_Guide_to_Termux.revised_master.editorial_pass_4.md",
        DOCS / "Hitchhikers_Guide_to_Termux.all_animation_sequences.md",
    ]
    for slug, rx in groups.items():
        scenes: dict[str, list[Path]] = defaultdict(list)
        for name, paths in source_index.items():
            match = rx.match(name)
            if not match:
                continue
            for path in paths:
                scenes[match.group(1)].append(path)
        if not scenes:
            continue

        animation_dir = OUT / "animations" / "legacy" / slug
        command = "ls" if slug.startswith("ls") else slug
        prose = command_sections(command, prose_sources)
        intro = (
            f"# Legacy animation material — {slug}\n\n"
            "This entry has scene reference media in the repository but is not represented by a current 5-second production packet. "
            "The command prose found elsewhere in the Guide is collected below; scene folders preserve only the media and scene-local inventory because no authoritative per-scene script was found.\n"
        )
        write(animation_dir / "animation.md", intro + ("\n" + prose if prose else ""))
        scene_records = []
        for cut, refs in sorted(scenes.items()):
            scene_dir = animation_dir / "scenes" / cut
            ordered = sorted(refs, key=lambda p: p.name)
            write(
                scene_dir / "scene.md",
                f"# Scene {cut}\n\n"
                "No authoritative scene-specific text was found for this legacy sequence.\n\n"
                "## References\n\n"
                + "\n".join(f"- `{p.name}`" for p in ordered),
            )
            copied: list[str] = []
            for ref in ordered:
                dst = scene_dir / "references" / ref.name
                copy_file(ref, dst)
                copied.append(ref.relative_to(ROOT).as_posix())
            write(
                scene_dir / "references" / "README.md",
                "# Scene reference inventory\n\n" + "\n".join(f"- `{item}`" for item in copied),
            )
            scene_records.append({"scene": cut, "references_found": copied})
        manifest["legacy"].append({"slug": slug, "scenes": scene_records})


def copy_unassigned_media(source_index: dict[str, list[Path]], manifest: dict) -> None:
    assigned_names: set[str] = set()
    for category in ("canonical", "tangents", "legacy"):
        for animation in manifest[category]:
            for scene in animation.get("scenes", []):
                for item in scene.get("references_found", []):
                    assigned_names.add(Path(item).name)

    candidate_rx = re.compile(
        r"(?:_clip_|^cartography_\d+|^cat_\d+|^elevator_\d+|^package_\d+|mustard|protest|workers?|^ChatGPT Image |\.mp4$)",
        re.I,
    )
    target = OUT / "project" / "references" / "unassigned"
    for name, paths in sorted(source_index.items()):
        if name in assigned_names or not candidate_rx.search(name):
            continue
        for path in paths:
            if path.suffix.lower() not in MEDIA_SUFFIXES or is_under(path, OUT):
                continue
            copy_file(path, target / path.name)
            manifest["unassigned_references"].append(path.relative_to(ROOT).as_posix())


def build_readme(manifest: dict, project_files: list[str]) -> None:
    canonical_scenes = sum(len(a["scenes"]) for a in manifest["canonical"])
    tangent_scenes = sum(len(a["scenes"]) for a in manifest["tangents"])
    legacy_scenes = sum(len(a["scenes"]) for a in manifest["legacy"])
    text = f"""# HG2Gui Guide material

This is the organized, non-destructive collection of Guide-related production material. Source files remain in their original locations so application and documentation paths do not break.

## Structure

- `project/` — Guide-wide prose, design/motion references, shared production rules, relevant source-code copies, and unresolved references.
- `animations/canonical/` — the 35 current technical 5-second production-packet entries.
- `animations/tangents/` — the four scripted Guide tangents that precede canonical entries.
- `animations/legacy/` — older/incomplete animated material that has reference media but no current production packet.

Every animation folder contains its complete animation text. Every `scenes/NN/scene.md` contains only material belonging to that scene. Scene-specific reference files are copied into that scene's `references/` folder. Shared design references remain under `project/references/shared/` rather than being multiplied hundreds of times without evidence that they belong to one particular scene.

## Inventory

- Canonical animations: **{len(manifest['canonical'])}**
- Canonical scenes: **{canonical_scenes}**
- Tangent animations: **{len(manifest['tangents'])}**
- Tangent scenes: **{tangent_scenes}**
- Legacy/incomplete animation groups: **{len(manifest['legacy'])}**
- Legacy scenes: **{legacy_scenes}**
- Project-wide files copied: **{len(project_files)}**
- Unassigned scene/reference media retained for review: **{len(manifest['unassigned_references'])}**

`MANIFEST.json` records provenance and reports reference frames named in the cut sheet that are planned but not actually present in the repository.
"""
    write(OUT / "README.md", text)


def main() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)

    source_index = build_source_index()
    OUT.mkdir(parents=True, exist_ok=True)

    manifest = {
        "canonical": [],
        "tangents": [],
        "legacy": [],
        "unassigned_references": [],
    }

    project_files = copy_project_material(source_index)
    organize_canonical(source_index, manifest)
    organize_tangents(source_index, manifest)
    organize_legacy(source_index, manifest)
    copy_unassigned_media(source_index, manifest)
    build_readme(manifest, project_files)
    write(OUT / "MANIFEST.json", json.dumps(manifest, indent=2, ensure_ascii=False))

    print(
        f"organized {len(manifest['canonical'])} canonical animations, "
        f"{len(manifest['tangents'])} tangents, and {len(manifest['legacy'])} legacy groups into {OUT.relative_to(ROOT)}"
    )


if __name__ == "__main__":
    main()
