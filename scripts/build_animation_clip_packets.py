#!/usr/bin/env python3
"""Build self-contained Guide animation clip packets from verified source material.

This is called after organize_guide_material.py recreates docs/guide.
Never manufacture missing boundary images or narration for legacy clips.
"""
from __future__ import annotations

import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GUIDE = ROOT / "docs/guide"
CUT_SHEET = ROOT / "docs/HG2Gui_animation_5s_cut_sheet_v2.md"

STYLE = (
    "EXACTLY 5.0 seconds. ORIGINAL 1:1 square flat 2D HG2Gui Guide animation "
    "in the screen-printed cut-paper pictogram language of the 2005 Hitchhiker's Guide "
    "(do not copy film designs). One uninterrupted matte flat background color, edge to edge; "
    "limited matte palette, sparse geometry, flattened perspective, deadpan comedy, crisp "
    "linear slides/pivots/flips with slight printed-edge texture. NO app chrome, chapter "
    "tabs, banners, borders, black bars, labels or captions added by the production process. "
    "Text ONLY when narratively required as an in-scene sign or command; reproduce scripted "
    "words exactly. No subtitles, gradients, glossy surfaces, bloom, shadows, camera shake, "
    "depth of field, 3D, photorealism, anime, sci-fi HUD, dissolves or bouncy easing. "
    "Preserve established character and prop silhouettes, proportions, costume and palette "
    "exactly. One major visual action per clip. Reach exact end composition at 4.6 seconds "
    "and hold to 5.0; no fade."
)


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.rstrip() + "\n", encoding="utf-8")


def cut_map() -> dict[int, dict]:
    content = CUT_SHEET.read_text(encoding="utf-8")
    headings = list(re.finditer(r"(?m)^## (\d{2}) — .+$", content))
    output: dict[int, dict] = {}
    for idx, heading in enumerate(headings):
        section = content[heading.start():headings[idx + 1].start() if idx + 1 < len(headings) else len(content)]
        background = re.search(r"\*\*Background:\*\*\s*([^\n]+)", section)
        pieces = list(re.finditer(r"(?m)^### Cut (\d{2}) — ([^\n]+)$", section))
        clips: dict[int, dict] = {}
        for j, cut in enumerate(pieces):
            text = section[cut.start():pieces[j + 1].start() if j + 1 < len(pieces) else len(section)]
            start = re.search(r"\*\*START:\*\*\s*\x60([^\x60]+)\x60", text)
            end = re.search(r"\*\*END:\*\*\s*\x60([^\x60]+)\x60", text)
            action = re.sub(r"(?m)^\*\*(?:START|END):\*\*[^\n]*\n", "", text)
            action = re.sub(r"^### Cut[^\n]*\n", "", action).strip()
            clips[int(cut.group(1))] = {
                "start": start.group(1) if start else None,
                "end": end.group(1) if end else None,
                "beat": action,
                "time": cut.group(2),
            }
        output[int(heading.group(1))] = {
            "background": background.group(1) if background else "the sequence's existing background",
            "clips": clips,
        }
    return output


def build() -> tuple[int, int]:
    manifest = json.loads((GUIDE / "MANIFEST.json").read_text(encoding="utf-8"))
    cuts = cut_map()
    groups = (
        ("canonical", manifest["canonical_animations"]),
        ("tangents", manifest["tangent_animations"]),
        ("legacy", manifest["legacy_animations"]),
    )
    index = [
        "# Guide animation clip production index", "",
        "Production-ready packets by animation and clip. Each clip folder contains its script excerpt, "
        "video-generation prompt, verified START/END shots when present, and source attribution. "
        "Original animation, scene, and storyboard source files remain unchanged.",
        "", "[Style lock](../HG2Gui_animation_style_lock_prompt_v2.md) · "
        "[Missing-frame checklist](ANIMATION_TODO.md) · [Full provenance](MANIFEST.json)",
        "", "**Missing images and unverified legacy narration are not fabricated.**", "",
    ]
    clip_count = frame_count = 0
    for kind, animations in groups:
        label = {"canonical": "Canonical", "tangents": "Tangents", "legacy": "Legacy"}[kind]
        index.extend((f"## {label} ({len(animations)})", ""))
        for animation in animations:
            folder = animation["folder"]
            name = animation.get("command") or animation.get("name") or Path(folder).name
            rows = [
                f"# {name} — production clips", "",
                "[Source animation](animation.md) · [Existing storyboard](storyboard.md)", "",
                "| Clip | START | END | Script | Prompt |", "| --- | --- | --- | --- | --- |",
            ]
            for scene in animation["scenes"]:
                number = f"{int(scene['scene']):02d}"
                destination = ROOT / folder / "clips" / number
                cut = cuts.get(animation.get("index"), {}).get("clips", {}).get(int(scene["scene"]))
                if kind == "canonical":
                    if not cut or any(cut[role] != scene["storyboard"][role]["expected"] for role in ("start", "end")):
                        raise ValueError(f"Cut sheet and manifest disagree on {name} clip {number}")
                clip_count += 1
                beat = cut["beat"] if kind == "canonical" else scene["narration"] if kind == "tangents" else None
                boundaries = scene["storyboard"]

                def exists(role: str) -> bool:
                    b = boundaries[role]
                    return bool(b.get("local")) and b["status"].startswith("present") and (ROOT / b["local"]).is_file()

                def status(role: str) -> str:
                    return "present" if exists(role) else boundaries[role].get("status") or "unresolved"

                def expected(role: str) -> str:
                    return boundaries[role].get("expected") or "not declared"

                for role in ("start", "end"):
                    output = destination / (role + ".png")
                    if exists(role):
                        output.parent.mkdir(parents=True, exist_ok=True)
                        shutil.copy2(ROOT / boundaries[role]["local"], output)
                        frame_count += 1
                    elif output.exists():
                        output.unlink()

                script_source = (
                    "`docs/HG2Gui_animation_5s_cut_sheet_v2.md`" if kind == "canonical"
                    else "the original tangent cut map" if kind == "tangents"
                    else "no verified clip script"
                )
                script_kind = (
                    "Visual action (verbatim cut sheet)" if kind == "canonical"
                    else "Narration beat (verbatim cut map)" if kind == "tangents"
                    else "Unresolved source script"
                )
                script_text = beat or (
                    "No verified clip-level script was found for this legacy boundary pair. "
                    "No dialogue or plot is inferred from filename alone."
                )
                write(destination / "script.md",
                      f"# {name} — clip {number}: script\n\n"
                      f"**Source:** {script_source}\n\n**{script_kind}:**\n\n{script_text}")

                def guard(role: str) -> str:
                    if exists(role):
                        detail = f"Use the supplied {role}.png exactly; match pose, layout, proportions and colors."
                    else:
                        detail = (
                            f"{status(role)}, expected {expected(role)}. A real approved {role} image "
                            "must exist before producing the final matched render."
                        )
                    return f"{role.upper()}: {detail}"

                if kind == "canonical":
                    action = "ANIMATE THIS VISUAL ACTION EXACTLY:\n" + beat
                    background = "Locked background: " + cuts[animation["index"]]["background"]
                elif kind == "tangents":
                    action = "VISUALIZE THIS DOCUMENTED NARRATION BEAT (not as subtitles):\n" + beat
                    background = "Use the unchanged background color visible in the supplied frames."
                else:
                    action = (
                        "UNSCRIPTED LEGACY CLIP: animate the shortest plausible continuous graphic "
                        "movement directly from the provided START image to the END image; invent no "
                        "events, signs, characters, dialogue, or narration."
                    )
                    background = "Use the unchanged background color visible in the supplied frames."
                write(destination / "prompt.md",
                      f"# {name} — clip {number}: generation prompt\n\n"
                      "**Source script:** [script.md](script.md)\n\n~~~text\n"
                      f"{STYLE}\n\n{background}\n{guard('start')}\n{guard('end')}\n"
                      "If a boundary is unavailable, this is a planning prompt only; generate and "
                      "approve the missing frame first, never silently substitute.\n\n"
                      f"{action}\n\nFor shared consecutive boundaries, use exactly the SAME approved "
                      "image file, not a regenerated lookalike.\n~~~")

                def boundary_row(role: str) -> str:
                    b = boundaries[role]
                    rendered = f"[image]({role}.png)" if exists(role) else "not available"
                    origins = ", ".join(f"\x60{source}\x60" for source in b.get("sources", []))
                    return f"| {role.upper()} | \x60{expected(role)}\x60 | {status(role)} | {rendered} | {origins} |"

                duration = (
                    f"5.0 seconds · {cut['time']}" if kind == "canonical"
                    else "5.0 seconds (declared by cut map)" if kind == "tangents"
                    else "unspecified in source (not assumed)"
                )
                note = ""
                if kind == "tangents" and scene["scene"] == 1:
                    note = (
                        "The original tangent cut map supplies an END still for the first cut "
                        "but no START still; that boundary is unresolved."
                    )
                elif kind == "legacy":
                    note = (
                        "No current five-second production packet or per-clip narration exists "
                        "for this legacy pair; the images are verified but the story beat is not."
                    )
                write(destination / "README.md",
                      f"# {name} — clip {number}\n\n**Duration:** {duration}  \n"
                      f"**Original scene:** [scene.md](../../scenes/{number}/scene.md)  \n"
                      "**Animation script:** [animation.md](../../animation.md)\n\n"
                      "| Boundary | Expected source | Status | Image | Provenance |\n"
                      "| --- | --- | --- | --- | --- |\n"
                      + boundary_row("start") + "\n" + boundary_row("end")
                      + "\n\n[Clip script](script.md) · [Clip generation prompt](prompt.md)\n\n" + note)

                def listing_frame(role: str) -> str:
                    if exists(role):
                        return f"[image](clips/{number}/{role}.png)"
                    return f"**{status(role)}**"

                rows.append(f"| [{number}](clips/{number}/README.md) | {listing_frame('start')} | "
                            f"{listing_frame('end')} | [script](clips/{number}/script.md) | "
                            f"[prompt](clips/{number}/prompt.md) |")
            write(ROOT / folder / "CLIPS.md", "\n".join(rows))
            idx_path = folder.removeprefix("docs/guide/")
            index.append(f"- [{name}]({idx_path}/CLIPS.md) — {len(animation['scenes'])} clips")
        index.append("")
    write(GUIDE / "CLIP_PRODUCTION_INDEX.md", "\n".join(index))
    return clip_count, frame_count


if __name__ == "__main__":
    clips, frames = build()
    print(f"Generated {clips} clip packets with {frames} verified boundary images.")
