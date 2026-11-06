#!/usr/bin/env python3
"""Build the Dreamina Seedance 2.5 pack for the cat tangent (`23_cat_theological_mechanical_history`).

The tangent runs 105 seconds, longer than one Seedance generation, so it is built as SEVEN generations of
15 seconds (three 5-second cuts each). Each part opens on one approved boundary frame and closes on another;
neighbouring parts share a frame, so part N+1 opens on exactly what part N closes on.

The zip holds a README, an INDEX and one folder per part:

  part_K_cuts_AA-BB/prompt.txt     format and design specs, reference roles, the scene description, the script
  part_K_cuts_AA-BB/references/    the four boundary frames of the part, then the character sheet (@Image1 ...)

Sources (read only, never edited):
  docs/guide/animations/tangents/23_cat_theological_mechanical_history/animation.md      narration beat per cut
  docs/guide/animations/tangents/23_cat_theological_mechanical_history/IMAGE_PROMPTS.md  composition of each frame
  docs/cat_tangent_frame_00.png .. _21.png and docs/cat_tangent_character_sheet.png

    python3 scripts/build_seedance_cat_tangent.py --out /path/to/output

Standard library only. The zip is reproducible (fixed timestamps, stable order).
"""
from __future__ import annotations

import argparse
import importlib.util
import re
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DOCS = REPO / "docs"
FOLDER = "23_cat_theological_mechanical_history"
TANGENT = REPO / "docs/guide/animations/tangents" / FOLDER
SHEET = DOCS / "cat_tangent_character_sheet.png"
CUT_SECONDS = 5
CUTS_PER_PART = 3
ZIP_TIME = (2026, 1, 1, 0, 0, 0)
PALETTE = (
    "cream (the cat and cut-paper figures), navy-black ink, brick red and mustard accents, plus one flat background "
    "colour per beat taken from the reference images"
)
TITLE = "CAT — A BRIEF THEOLOGICAL AND MECHANICAL HISTORY"


def _load_pack_module():
    spec = importlib.util.spec_from_file_location("build_seedance_packs", REPO / "scripts/build_seedance_packs.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


PACKS = _load_pack_module()

FORMAT = """\
FORMAT AND FRAME
- Exactly 1:1 square for the whole generation. {seconds} seconds, one continuous generation made of {beats} consecutive {cut}-second beats.
- Every moment has ONE single flat background colour filling the entire square, edge to edge, exactly the colour of the reference image for that moment. Where two neighbouring reference images differ in background colour, the background changes by one hard flat colour wipe during the beat, never a gradient, fade or dissolve. No second background band, no strips, no vignette, no panel.
- NO header, NO tabs, NO title, NO banner, NO border, NO frame, NO letterboxing, NO mattes, NO window controls, NO interface chrome, NO timestamps, NO shot numbers, NO production labels, NO subtitles or captions. The animation is only the artwork on the flat background.
- No text anywhere. The marks on the clay tablet and the symbols on Egyptian-style signs are decorative shapes that carry no message.
- No narrator, no voice-over and no spoken dialogue."""

CONTINUITY = """\
CONTINUITY
- The cat is always the same character: a flat cream cutout with a round head, two small triangular ears, thin cream whiskers, tiny dark dot eyes and a small dark nose. Where a reference image shows a black cat, keep that cat exactly as drawn there. Silhouette, proportions, colour placement, prop geometry and scale never drift from the references.
- Each beat starts exactly on its opening reference image and ends exactly on its closing reference image, which is also the opening image of the next beat. Animate the shortest clear flat transformation from one image to the next, then hold the closing composition for the last 0.4 seconds.
- The beats join with no visible seam: no fade-in, no fade-out, no cut to black, no title card, no transition graphics.
- Do not invent dialogue, captions, labels or story events beyond the scene description and script."""


@dataclass
class Frame:
    index: int
    composition: str
    background: str  # e.g. "mustard (#D9A12B)"


@dataclass
class Part:
    number: int
    first_cut: int
    last_cut: int
    frames: list[Frame]
    beats: dict[int, str]  # cut number -> narration

    @property
    def folder(self) -> str:
        return f"part_{self.number}_cuts_{self.first_cut:02d}-{self.last_cut:02d}"

    @property
    def seconds(self) -> int:
        return (self.last_cut - self.first_cut + 1) * CUT_SECONDS


def parse_frames() -> dict[int, Frame]:
    text = (TANGENT / "IMAGE_PROMPTS.md").read_text(encoding="utf-8")
    frames: dict[int, Frame] = {}
    for match in re.finditer(r"### Frame (\d\d) — [^\n]*\n.*?~~~text\n(.*?)\n~~~", text, re.S):
        index, body = int(match.group(1)), match.group(2).strip()
        colour = re.search(r"Flat ([a-z ]+?) \((#[0-9A-Fa-f]{6})\) background", body)
        if not colour:
            raise ValueError(f"frame {index:02d}: no background colour in its prompt")
        frames[index] = Frame(index, body, f"{colour.group(1)} ({colour.group(2).upper()})")
    if sorted(frames) != list(range(22)):
        raise ValueError(f"expected frames 00-21, found {sorted(frames)}")
    return frames


def parse_beats() -> dict[int, str]:
    text = (TANGENT / "animation.md").read_text(encoding="utf-8")
    rows = re.findall(r"^\| (\d\d) \| `cat_\d\d\.png` \| (.+) \|$", text, re.M)
    beats = {int(n): t.strip() for n, t in rows}
    if sorted(beats) != list(range(1, 22)):
        raise ValueError(f"expected cuts 01-21 in the cut map, found {sorted(beats)}")
    return beats


def build_parts() -> list[Part]:
    frames, beats = parse_frames(), parse_beats()
    parts = []
    for number in range(1, 8):
        first = (number - 1) * CUTS_PER_PART + 1
        last = first + CUTS_PER_PART - 1
        parts.append(Part(
            number, first, last,
            [frames[i] for i in range(first - 1, last + 1)],
            {cut: beats[cut] for cut in range(first, last + 1)},
        ))
    return parts


def frame_file(index: int) -> Path:
    return DOCS / f"cat_tangent_frame_{index:02d}.png"


def build_prompt(part: Part) -> str:
    beats = len(part.beats)
    lines = [
        f"{TITLE} — part {part.number} of 7: one continuous {part.seconds}-second animation in {beats} consecutive beats.",
        "",
        FORMAT.format(seconds=part.seconds, beats=beats, cut=CUT_SECONDS),
        "",
        PACKS.STYLE_BLOCK.format(palette=PALETTE),
        "",
        CONTINUITY,
        "",
        "REFERENCE IMAGES (uploaded in this order)",
    ]
    last_image = len(part.frames)
    for position, frame in enumerate(part.frames, start=1):
        at = f"{(position - 1) * CUT_SECONDS}s"
        if position == 1:
            role = f"OPENING FRAME: the exact composition at {at}. Begin exactly on it."
        elif position == last_image:
            role = f"CLOSING FRAME: the exact composition at {at}. Finish exactly on it and hold."
        else:
            role = f"the exact composition at {at} (the end of beat {position - 1} and the start of beat {position})."
        lines.append(f"- @Image{position}: {role} {frame.composition}")
    lines.append(
        f"- @Image{last_image + 1}: CHARACTER AND PROP REFERENCE, not a frame: the cat in four poses (sitting, hanging "
        "upside down, lying on its back, walking in profile) and a small crown, an armchair and an open box. Match the "
        "cat's design to it. It carries no text."
    )
    lines += ["", "SCENE DESCRIPTION (complete, in order)"]
    for position, cut in enumerate(sorted(part.beats), start=1):
        start, end = (position - 1) * CUT_SECONDS, position * CUT_SECONDS
        lines += [
            f"Beat {position} of {beats} — {start}s–{end}s",
            f"Move from @Image{position} to @Image{position + 1}. Story beat: {part.beats[cut]}",
            "",
        ]
    lines.append("SCRIPT FOR THIS PART (verbatim from the Guide; context only, never spoken, shown or written on screen)")
    lines += [part.beats[cut] for cut in sorted(part.beats)]
    return "\n".join(lines).rstrip() + "\n"


def build_readme(parts: list[Part], prompt_sizes: dict[int, int]) -> str:
    lines = [
        f"{TITLE} — Dreamina Seedance 2.5 pack",
        "",
        "WHY SEVEN PARTS",
        "  The tangent runs 105 seconds; Seedance generates 4-30 seconds at a time. It is split on cut boundaries into",
        "  seven 15-second generations of three cuts each. Parts share boundary frames, so part N+1 opens on the exact",
        "  image part N closes on. Generate them in order and join them end to end.",
        "",
        "SETTINGS (every part)",
        "  Aspect ratio: 1:1        Duration: 15 seconds        No header, tabs or chrome.",
        "  The prompt-length limit is not stated in the sources I found; if Seedance rejects a prompt, trim the VISUAL",
        "  DESIGN section first and keep the reference roles, scene description and script.",
        "",
        "HOW TO USE (each part_* folder)",
        "  1. Upload the images in references/ in file-name order, so @Image1 ... @Image5 match the prompt.",
        "  2. Paste prompt.txt as the prompt.",
        "  3. If you use first-frame / last-frame mode instead, use image 1 as the opening frame and image 4 as the last",
        "     frame. I could not confirm that Seedance 2.5 allows first/last-frame mode and reference images together.",
        "",
        "PARTS",
    ]
    for part in parts:
        lines.append(
            f"  {part.folder:28} {part.seconds}s  frames {part.frames[0].index:02d}-{part.frames[-1].index:02d}  "
            f"{prompt_sizes[part.number]} characters"
        )
    lines += [
        "",
        "THINGS TO KNOW",
        "  - The frames change background colour from beat to beat. Seedance may soften the colour changes. To use one",
        "    colour for the whole tangent, redraw the frames in that colour first; the compositions do not depend on it.",
        "  - Frame 02 (part 1) and frame 05 (part 2) show a black cat by design; every other frame has the cream cat.",
        "    Watch parts 1 and 2 for the cat's colour drifting. Frames 19-21 keep the cat small in front of the colonnade.",
        "  - Cost: ask Dreamina for the credit price of a 15-second 1:1 generation before running all seven. I have not",
        "    confirmed Seedance 2.5 pricing.",
        "",
        "SOURCES",
        f"  docs/guide/animations/tangents/{FOLDER}/animation.md and IMAGE_PROMPTS.md;",
        "  docs/cat_tangent_frame_00.png to _21.png and docs/cat_tangent_character_sheet.png.",
    ]
    return "\n".join(lines) + "\n"


def write_zip(parts: list[Part], out_dir: Path) -> Path:
    root = f"{FOLDER}_seedance2.5"
    target = out_dir / f"{root}.zip"
    prompts = {part.number: build_prompt(part) for part in parts}

    def add(zf: zipfile.ZipFile, name: str, data: bytes, compress: int) -> None:
        info = zipfile.ZipInfo(f"{root}/{name}", ZIP_TIME)
        info.compress_type = compress
        info.external_attr = 0o644 << 16
        zf.writestr(info, data)

    with zipfile.ZipFile(target, "w") as zf:
        add(zf, "README.txt", build_readme(parts, {n: len(p) for n, p in prompts.items()}).encode("utf-8"), zipfile.ZIP_DEFLATED)
        for part in parts:
            add(zf, f"{part.folder}/prompt.txt", prompts[part.number].encode("utf-8"), zipfile.ZIP_DEFLATED)
            for position, frame in enumerate(part.frames, start=1):
                name = f"{position:02d}_frame_{frame.index:02d}_at_{(position - 1) * CUT_SECONDS}s.png"
                add(zf, f"{part.folder}/references/{name}", frame_file(frame.index).read_bytes(), zipfile.ZIP_STORED)
            sheet_name = f"{len(part.frames) + 1:02d}_character_sheet.png"
            add(zf, f"{part.folder}/references/{sheet_name}", SHEET.read_bytes(), zipfile.ZIP_STORED)
    return target


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", required=True, type=Path, help="directory to write the zip into")
    args = parser.parse_args(argv)

    missing = [str(p) for p in [SHEET, *(frame_file(i) for i in range(22))] if not p.is_file()]
    if missing:
        print("missing images:\n  " + "\n  ".join(missing), file=sys.stderr)
        return 2
    args.out.mkdir(parents=True, exist_ok=True)
    path = write_zip(build_parts(), args.out)
    print(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
