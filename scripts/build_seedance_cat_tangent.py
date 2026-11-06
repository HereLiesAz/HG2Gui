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

    python3 scripts/build_seedance_cat_tangent.py --out /path/to/output [--per-part]

--per-part writes seven self-contained zips (prompt, README, five images each) instead of one zip with seven folders.
Seedance 2.0 takes at most 9 images and 4-15 seconds per generation; each part uses five images and 15 seconds.

Standard library only. The zip is reproducible (fixed timestamps, stable order).
"""
from __future__ import annotations

import argparse
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


MAX_PROMPT = 4000  # Seedance 2.0 rejects prompts longer than this many characters

FORMAT = (
    "FORMAT: Exactly 1:1 square, {seconds} seconds, one continuous generation of {beats} consecutive {cut}-second beats. "
    "Every moment has ONE flat background colour edge to edge, matching the reference image for that moment; where "
    "neighbouring references differ, change colour with one hard flat wipe, never a fade or gradient. No header, tabs, "
    "title, border, frame, letterbox, captions, timestamps or any text; marks on tablets or signs are decorative shapes. "
    "No narration, voice or dialogue."
)

STYLE = (
    "STYLE: Flat 2D screen-printed cut-paper pictogram in the visual language of the 2005 Hitchhiker's Guide graphics: "
    "economical geometric silhouettes, bold negative space, matte colour, slightly imperfect printed edges, dry deadpan "
    "staging. Opaque flat cutouts only: no gradients, gloss, shadows, 3D, depth of field or photorealism. Motion is crisp "
    "and mostly linear: slides, pivots, flips, flat pans and zooms; no camera shake, soft dissolves or bouncy easing. "
    "Palette: cream, navy-black, brick red, mustard, plus each beat's background colour."
)

CONTINUITY = (
    "CONTINUITY: The cat is one character throughout: flat cream cutout, round head, two small triangular ears, thin "
    "whiskers, tiny dark dot eyes and nose. Where a reference shows a black cat, keep it exactly as drawn. Props, "
    "silhouettes, colours and scale never drift from the references. Each beat starts exactly on its opening reference "
    "and ends exactly on its closing reference (also the next beat's opening) by the shortest clear flat transformation, "
    "then holds that composition for the last 0.4 seconds. No fades, cuts to black, title cards or transition graphics. "
    "Invent no events beyond the beats."
)


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
    last_image = len(part.frames)
    lines = [
        f"{TITLE}, part {part.number} of 7.",
        FORMAT.format(seconds=part.seconds, beats=beats, cut=CUT_SECONDS),
        STYLE,
        CONTINUITY,
        "REFERENCE IMAGES (upload in this order):",
    ]
    for position, frame in enumerate(part.frames, start=1):
        at = (position - 1) * CUT_SECONDS
        if position == 1:
            role = f"opening frame at {at}s, begin exactly on it"
        elif position == last_image:
            role = f"closing frame at {at}s, finish exactly on it and hold"
        else:
            role = f"exact composition at {at}s (end of beat {position - 1}, start of beat {position})"
        lines.append(f"@Image{position}: {role}; background {frame.background}.")
    lines.append(
        f"@Image{last_image + 1}: character and prop sheet (cat in four poses, a crown, an armchair, a box), "
        "not a frame; match the cat to it. No text."
    )
    lines.append("BEATS:")
    for position, cut in enumerate(sorted(part.beats), start=1):
        lines.append(
            f"Beat {position} ({(position - 1) * CUT_SECONDS}-{position * CUT_SECONDS}s): @Image{position} to "
            f"@Image{position + 1}. Story: {part.beats[cut]}"
        )
    text = "\n".join(lines) + "\n"
    if len(text) > MAX_PROMPT:
        raise ValueError(f"part {part.number}: prompt is {len(text)} characters, over the {MAX_PROMPT} limit")
    return text


def build_readme(parts: list[Part], prompt_sizes: dict[int, int]) -> str:
    lines = [
        f"{TITLE} — Dreamina Seedance pack",
        "",
        "WHY SEVEN PARTS",
        "  The tangent runs 105 seconds. Seedance 2.0 generates 4-15 seconds at a time and takes at most 9 images, so it is",
        "  split on cut boundaries into seven 15-second generations of three cuts each, five images apiece. Parts share",
        "  boundary frames, so part N+1 opens on the exact image part N closes on. Generate them in order and join them",
        "  end to end.",
        "",
        "SETTINGS (every part)",
        "  Aspect ratio: 1:1        Duration: 15 seconds        No header, tabs or chrome.",
        "  Seedance 2.0 allows at most 4,000 characters per prompt; every prompt here is under that (the build fails if not).",
        "",
        "HOW TO USE (each part_* folder)",
        "  1. Upload the images in references/ in file-name order, so @Image1 ... @Image5 match the prompt.",
        "  2. Paste prompt.txt as the prompt.",
        "  3. Third-party sources say first/last-frame mode and multi-image reference mode cannot be mixed. If you use",
        "     first/last-frame mode instead, use image 1 as the first frame and image 4 as the last, and upload nothing else.",
        "",
        "PARTS",
    ]
    for part in parts:
        lines.append(
            f"  {part.folder:28} {part.seconds}s  frames {part.frames[0].index:02d}-{part.frames[-1].index:02d}  "
            f"{prompt_sizes[part.number]} of 4000 characters"
        )
    lines += [
        "",
        "THINGS TO KNOW",
        "  - The frames change background colour from beat to beat. Seedance may soften the colour changes. To use one",
        "    colour for the whole tangent, redraw the frames in that colour first; the compositions do not depend on it.",
        "  - Frame 02 (part 1) and frame 05 (part 2) show a black cat by design; every other frame has the cream cat.",
        "    Watch parts 1 and 2 for the cat's colour drifting. Frames 19-21 keep the cat small in front of the colonnade.",
        "  - Cost: ask Dreamina for the credit price of a 15-second 1:1 generation before running all seven. I have not",
        "    confirmed Seedance pricing.",
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


def build_part_readme(part: Part, prompt_chars: int) -> str:
    return "\n".join([
        f"{TITLE} — part {part.number} of 7 (cuts {part.first_cut:02d}-{part.last_cut:02d}, frames "
        f"{part.frames[0].index:02d}-{part.frames[-1].index:02d})",
        "",
        "SETTINGS",
        "  Aspect ratio: 1:1        Duration: 15 seconds        No header, tabs or chrome.",
        f"  Images: {len(part.frames) + 1} (Seedance 2.0 takes at most 9).   Prompt: {prompt_chars} of 4000 characters.",
        "",
        "HOW TO USE",
        "  1. Upload the images in references/ in file-name order, so @Image1 ... @Image5 match the prompt.",
        "  2. Paste prompt.txt as the prompt.",
        "  3. First/last-frame mode instead: use image 1 as the first frame and image 4 as the last, and upload nothing else.",
        "     Third-party sources say that mode cannot be mixed with reference images.",
        "",
        "ORDER",
        (f"  Generate after part {part.number - 1}: this part opens on the exact frame that part closes on."
         if part.number > 1 else "  This is the first part."),
        (f"  Part {part.number + 1} opens on this part's last image (image 4)." if part.number < 7 else "  This is the last part."),
        "",
    ])


def write_part_zips(parts: list[Part], out_dir: Path) -> list[Path]:
    """One self-contained zip per part: its prompt, a short README and its five images."""
    targets = []
    for part in parts:
        root = f"cat_tangent_{part.folder}"
        target = out_dir / f"{root}.zip"
        prompt = build_prompt(part)

        def add(zf: zipfile.ZipFile, name: str, data: bytes, compress: int) -> None:
            info = zipfile.ZipInfo(f"{root}/{name}", ZIP_TIME)
            info.compress_type = compress
            info.external_attr = 0o644 << 16
            zf.writestr(info, data)

        with zipfile.ZipFile(target, "w") as zf:
            add(zf, "prompt.txt", prompt.encode("utf-8"), zipfile.ZIP_DEFLATED)
            add(zf, "README.txt", build_part_readme(part, len(prompt)).encode("utf-8"), zipfile.ZIP_DEFLATED)
            for position, frame in enumerate(part.frames, start=1):
                name = f"{position:02d}_frame_{frame.index:02d}_at_{(position - 1) * CUT_SECONDS}s.png"
                add(zf, f"references/{name}", frame_file(frame.index).read_bytes(), zipfile.ZIP_STORED)
            add(zf, f"references/{len(part.frames) + 1:02d}_character_sheet.png", SHEET.read_bytes(), zipfile.ZIP_STORED)
        targets.append(target)
    return targets


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", required=True, type=Path, help="directory to write the zip(s) into")
    parser.add_argument("--per-part", action="store_true", help="write one self-contained zip per part instead of one zip")
    args = parser.parse_args(argv)

    missing = [str(p) for p in [SHEET, *(frame_file(i) for i in range(22))] if not p.is_file()]
    if missing:
        print("missing images:\n  " + "\n  ".join(missing), file=sys.stderr)
        return 2
    args.out.mkdir(parents=True, exist_ok=True)
    parts = build_parts()
    if args.per_part:
        for path in write_part_zips(parts, args.out):
            print(path)
    else:
        print(write_zip(parts, args.out))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
