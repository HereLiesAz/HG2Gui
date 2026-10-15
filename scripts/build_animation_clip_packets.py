#!/usr/bin/env python3
"""Build self-contained Guide animation clip packets from verified source material.

This is called after organize_guide_material.py refreshes the source inventory.
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
    "Flat 2D HG2Gui Guide animation "
    "in the screen-printed cut-paper pictogram language of the 2005 Hitchhiker's Guide "
    "(do not copy film designs). One uninterrupted matte flat background color, edge to edge; "
    "limited matte palette, sparse geometry, flattened perspective, deadpan comedy, crisp "
    "linear slides/pivots/flips with slight printed-edge texture. NO app chrome, chapter "
    "tabs, banners, borders, black bars, labels or captions added by the production process. "
    "Text ONLY when narratively required as an in-scene sign or command; reproduce scripted "
    "words exactly. No subtitles, gradients, glossy surfaces, bloom, shadows, camera shake, "
    "depth of field, 3D, photorealism, anime, sci-fi HUD, dissolves or bouncy easing. "
    "Preserve established character and prop silhouettes, proportions, costume and palette "
    "exactly. One major visual action per clip; no fade."
)

# The latest canonical cut-sheet requires square five-second outputs; the archived
# tangent screenshots and legacy shot pairs are NOT all square.
STYLE_CANONICAL = "Exactly 5.0 seconds, 1:1 square. Reach the endpoint at 4.6s; hold to 5.0s. "
STYLE_TANGENT = (
    "Five-second tangent cut. The numbered START still is an annotated, non-square "
    "storyboard screenshot, not an approved clean boundary. Extract or approve "
    "clean 1:1 artwork before rendering; do not preserve the visible START/END labels. "
)
STYLE_LEGACY = (
    "Legacy source duration is unverified: do not set a runtime or timed final hold "
    "until approved. Preserve the supplied native aspect ratio of both real images; "
    "do not crop, stretch, letterbox or force square dimensions. "
)


def write(path: Path, content: str, *, refresh: bool = False) -> None:
    """Keep curated scripts/prompts/shots; refresh indexes generated from the manifest."""
    path.parent.mkdir(parents=True, exist_ok=True)
    rendered = content.rstrip() + "\n"
    if path.exists():
        if not refresh or path.read_text(encoding="utf-8") == rendered:
            return
    path.write_text(rendered, encoding="utf-8")


def refresh_readme(path: Path, content: str, start_line: str, end_line: str) -> None:
    """Update frame availability without replacing hand-authored per-clip notes."""
    if not path.exists():
        write(path, content)
        return
    previous = path.read_text(encoding="utf-8")
    changed = previous
    # An existing missing-frame explanation may be hand-written; only touch it
    # when image availability actually changes, not on every rebuild.
    for role, wanted in (("start", start_line), ("end", end_line)):
        found = re.search(rf"(?m)^- {role.upper()}:.*$", changed)
        if not found:
            continue
        old_available = f"[{role}.png]" in found.group(0)
        new_available = f"[{role}.png]" in wanted
        if old_available != new_available:
            changed = changed[:found.start()] + wanted + changed[found.end():]
    if changed != previous:
        write(path, changed, refresh=True)


def refresh_shots(path: Path, boundaries: dict, available: dict[str, bool], kind: str) -> None:
    """Keep hand-authored shot descriptions, refreshing changed frame metadata only."""
    if not path.exists():
        return
    original = path.read_text(encoding="utf-8")
    result = original
    for role in ("start", "end"):
        heading = re.search(
            rf"(?ms)^## {role.upper()}(?: shot)?\n.*?(?=^## |\Z)", result
        )
        if not heading:
            continue
        section = heading.group(0)
        had_frame = bool(re.search(
            rf"(?m)^\*\*{role.upper()}\*\* — \[image\]", section
        ))
        if had_frame == available[role]:
            continue
        frame = boundaries[role]
        source = ", ".join(frame.get("sources") or []) or "not supplied"
        origin = (
            "annotated START screenshot" if kind == "tangents" and role == "start"
            else "original artwork" if available[role]
            else "no image supplied"
        )
        target = (
            f"**{role.upper()}** — [image]({role}.png)" if available[role]
            else f"**{role.upper()}** — NO IMAGE"
        )
        section = re.sub(
            rf"(?m)^\*\*{role.upper()}\*\* — .*?$",
            lambda _: target, section,
        )
        section = re.sub(r"(?m)^- Source: .*?$", lambda _: f"- Source: {source}", section)
        section = re.sub(r"(?m)^- Type: .*?$", lambda _: f"- Type: {origin}", section)
        result = result[:heading.start()] + section + result[heading.end():]
    if result != original:
        write(path, result, refresh=True)


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
        "# Animation production — by sequence and clip",
        "",
        "Each clip folder contains its own `shots.md`, `script.md`, `prompt.md`, and available "
        "original START/END images. Follow the per-animation indexes below; these files "
        "do not exist at the guide root.",
        "",
        "[Style rules](../00_READ_ME_FIRST.md) · [Cut sheet](../HG2Gui_animation_5s_cut_sheet_v2.md) · "
        "[Original provenance](MANIFEST.json) · [Missing-frame checklist](ANIMATION_TODO.md)",
        "",
        "**Tangent stills visibly contain START production labels: these are source references, "
        "not clean boundary images. No tangent END images have been verified.**", "",
    ]
    clip_count = frame_count = 0
    for kind, animations in groups:
        index.extend((f"## {kind}", ""))
        for animation in animations:
            folder = animation["folder"]
            name = animation.get("command") or animation.get("name") or Path(folder).name
            rows = [
                f"# {name} — production clips", "",
                "[Full animation script](animation.md) · [Storyboard](storyboard.md) · "
                "[Guide-wide clip index](../../../CLIP_PRODUCTION_INDEX.md)", "",
                "| Clip | Frames | Script excerpt | START/END shots | Animation prompt |",
                "| --- | --- | --- | --- | --- |",
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
                        if kind == "tangents":
                            detail = (
                                f"{role}.png is an annotated source START screenshot, not an exact "
                                "clean animation boundary. Approve clean 1:1 source art first."
                            )
                        else:
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
                style = STYLE + " " + {
                    "canonical": STYLE_CANONICAL,
                    "tangents": STYLE_TANGENT,
                    "legacy": STYLE_LEGACY,
                }[kind]
                write(destination / "prompt.md",
                      f"# {name} — clip {number}: generation prompt\n\n"
                      "**Source script:** [script.md](script.md)\n\n~~~text\n"
                      f"{style}\n\n{background}\n{guard('start')}\n{guard('end')}\n"
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
                if kind == "tangents":
                    note = (
                        "The tangent cut-map still is a labeled START source screenshot, not an "
                        "approved clean frame. The clip END still has not been supplied."
                    )
                elif kind == "legacy":
                    note = (
                        "No current five-second production packet or per-clip narration exists "
                        "for this legacy pair; the images are verified but the story beat is not."
                    )
                start_line = (
                    "- START: [start.png](start.png)" if exists("start")
                    else "- START: **missing** — see [shot specification](shots.md)"
                )
                end_line = (
                    "- END: [end.png](end.png)" if exists("end")
                    else "- END: **missing** — see [shot specification](shots.md)"
                )
                readme = (
                    f"# {name} — clip {number}\n\n**Duration:** {duration}\n\n"
                    f"{start_line}\n{end_line}\n"
                    "- [Corresponding script passage](script.md)\n"
                    "- [START/END shot specifications](shots.md)\n"
                    "- [Clip-specific animation prompt](prompt.md)\n\n"
                    + note
                )
                refresh_readme(destination / "README.md", readme, start_line, end_line)

                # A source-aligned shot record is required even for a newly introduced cut.
                # Preserve existing author-reviewed descriptions instead of replacing them.
                def shot_record(role: str) -> str:
                    b = boundaries[role]
                    origin = ", ".join(b.get("sources") or []) or "not supplied"
                    return (
                        f"## {role.upper()}\n\n"
                        f"- Source filename: `{expected(role)}`\n"
                        f"- Status: {status(role)}\n"
                        f"- Source location: {origin}\n"
                        + (f"- [Image]({role}.png)\n" if exists(role) else
                           "- No verified image available.\n")
                    )
                write(destination / "shots.md",
                      f"# {name} — clip {number}: START and END shot details\n\n"
                      + shot_record("start") + "\n" + shot_record("end")
                      + "\n**Documented action:**\n\n" + (beat or "Legacy clip action not documented.") +
                      "\n")

                refresh_shots(destination / "shots.md", boundaries,
                              {role: exists(role) for role in ("start", "end")}, kind)

                def listing_frame(role: str) -> str:
                    if exists(role):
                        return f"[image](clips/{number}/{role}.png)"
                    return f"**{status(role)}**"

                rows.append(f"| [{number}](clips/{number}/README.md) | {listing_frame('start')} → "
                            f"{listing_frame('end')} | [script](clips/{number}/script.md) | "
                            f"[shots](clips/{number}/shots.md) | "
                            f"[prompt](clips/{number}/prompt.md) |")
            write(ROOT / folder / "CLIPS.md", "\n".join(rows), refresh=True)
            idx_path = folder.removeprefix("docs/guide/")
            index.append(f"- [{name}]({idx_path}/CLIPS.md) — {len(animation['scenes'])} clips")
        index.append("")
    write(GUIDE / "CLIP_PRODUCTION_INDEX.md", "\n".join(index), refresh=True)
    return clip_count, frame_count


if __name__ == "__main__":
    clips, frames = build()
    print(f"Generated {clips} clip packets with {frames} verified boundary images.")
