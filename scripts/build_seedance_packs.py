#!/usr/bin/env python3
"""Build one Dreamina Seedance 2.5 pack (a zip) per canonical Guide animation.

Each zip holds the whole animation as ONE generation (not 5-second clips):

  prompt.txt     the complete prompt: format + visual design specs, reference-image roles, the full
                 scene description (every cut, with its time window), and the complete script.
  README.txt     upload order, settings, and anything the producer should know about this pack.
  references/    every reference image for the animation, numbered in upload order (@Image1 ...).

Sources are the repo's own files and are never edited:
  docs/guide/animations/canonical/NN_name/animation.md   script, palette, duration, per-cut scene text
  docs/guide/animations/canonical/NN_name/clips/*/prompt.md   the single background colour per entry
  docs/NN_name_frame_KK.png                               boundary frames, where they exist
  docs/guide/animations/canonical/NN_name/clips/*/references/*.png   other reference artwork

Format decisions (from the project owner): exactly 1:1, no header or tabs or any app chrome, and the whole
animation generated in a single pass rather than as separate 5-second clips.

    python3 scripts/build_seedance_packs.py --out /path/to/output [--only 01_pwd --only 02_pkg_install]

Standard library only. Zips are reproducible (fixed timestamps, stable order).
"""
from __future__ import annotations

import argparse
import hashlib
import re
import sys
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
CANONICAL = REPO / "docs/guide/animations/canonical"
DOCS = REPO / "docs"
ZIP_TIME = (2026, 1, 1, 0, 0, 0)
IMAGE_SUFFIXES = (".png", ".jpg", ".jpeg", ".webp")

# What each non-boundary reference image shows, by file name. Anything not listed gets GENERIC_ROLE.
# Descriptions come from looking at the images. They are in rough story order, but none is an exact frame.
REFERENCE_NOTES = {
    "directory_protesters_and_terminal_prompt.png":
        "a `pwd` command card, a line of workers holding blank red placards in front of the directory block, "
        "the foreman with a megaphone at left, the user at right",
    "imagegen.png":
        "a `pwd` command card, workers conferring in a huddle in front of the directory block, "
        "the foreman with a megaphone, the user at right",
    "tech_protest_workers_demand_change.png":
        "a `cd ..` command card with an up arrow, the placard `THEN STOP WORKING IN US` among the picket line, "
        "the foreman with a megaphone, the user at right",
    "workers_rise_with_a_command_prompt.png":
        "a `cd ..` command card, the picket line moving up an arrow toward the parent directory block, "
        "the foreman with a megaphone, the user at right",
    "command_line_protest_at_the_union_house.png":
        "a `cd ..` command card, a `THEN STOP WORKING IN US` sign on the ground, the picket line and foreman "
        "before the large parent directory block, a small directory block below",
    "mustard_design_system_character_sheet.png":
        "a CHARACTER AND PROP MODEL SHEET: the user, directory worker, foreman, worker group, current and parent "
        "directory blocks, sign, command cards and arrow. It carries production labels (USER_A, FOREMAN_A and so "
        "on) which must NEVER appear in the animation",
}
REFERENCE_ORDER = [
    "directory_protesters_and_terminal_prompt.png",
    "imagegen.png",
    "tech_protest_workers_demand_change.png",
    "workers_rise_with_a_command_prompt.png",
    "command_line_protest_at_the_union_house.png",
    "mustard_design_system_character_sheet.png",
]
GENERIC_ROLE = "supporting reference artwork for this animation"

# The reference images for 01_pwd are mustard, with navy, green and red figures; the generated prompt files
# elsewhere in the repo say navy #101D2D, which would hide the navy figures. The images win.
BACKGROUND_OVERRIDES = {
    "01_pwd": ("mustard", "#E39B1D", "sampled from the six reference images, which are all mustard"),
}

FORMAT_BLOCK = """\
FORMAT AND FRAME
- Exactly 1:1 square for the whole animation. {seconds} seconds, one continuous generation made of {beats} consecutive beats.
- ONE single flat background colour fills the entire square, edge to edge, for the whole animation: {bg_name} ({bg_hex}). No second background band, no strips, no vignette, no panel.
- NO header, NO tabs, NO title, NO banner, NO border, NO frame, NO letterboxing, NO mattes, NO window controls, NO interface chrome, NO timestamps, NO shot numbers, NO production labels, NO subtitles or captions. The animation is only the artwork on the flat background.{header_note}
- The only visible text is text the story itself puts on an object (a placard, command, sign, caption bubble or terminal strip), written exactly as it appears in the scene description. No other text anywhere.
- No narrator, no voice-over and no spoken dialogue."""

STYLE_BLOCK = """\
VISUAL DESIGN
- Minimalistic flat vector animation with simplified realism in the visual language of the 2005 Hitchhiker's Guide graphics: public-information-sign pictograms, economical silhouettes, bold negative space, matte colour, crisp clean edges, dry diagrammatic staging. Original compositions and original character designs; do not reproduce any existing film shot.
- Characters and props are flat opaque cutout shapes placed directly on the single-colour background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, creatures, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels, not from lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects; metaphors are treated as literal information graphics. Favour bold asymmetric composition and large areas of empty colour.
- Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabelling, flat pans and flat zooms. No camera shake, no soft dissolves, no bouncy modern easing; changes feel diagrammatic and inevitable.
- Comedy is dry and deadpan. Nobody performs the joke for the camera. The visual system explains a technically correct thing, follows one innocent word too literally, and lets the consequence become absurd.
- Use only 4 to 7 matte colours. Palette for this animation: {palette}.
- No generic outer space unless the story needs it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Follow the story's own joke, not the obvious software visualisation of the command. Do not add explanatory terminal output that the scene description does not contain.

CONTINUITY
- Every recurring character and prop keeps exactly the same silhouette, head and body proportions, clothing, colour placement, prop geometry and relative scale from the first second to the last. Only pose, position, orientation and the story's own state changes may differ.
- One major visual idea per beat. Each beat reaches its final composition about 0.4 seconds before the beat ends and holds it, so the next beat begins from a stable graphic state.
- The beats join with no visible seam: no fade-in, no fade-out, no cut to black, no title card, no transition graphics.
- Do not invent dialogue, captions, labels or story events beyond the scene description and script."""


@dataclass
class Cut:
    number: int
    start: str
    end: str
    text: str


@dataclass
class Animation:
    folder: str
    title: str
    chapter: str
    script: str
    editors_note: str | None
    palette: str
    seconds: int
    cuts: list[Cut]
    bg_name: str
    bg_hex: str
    bg_note: str | None


@dataclass
class RefImage:
    source: Path
    zip_name: str
    role: str
    kind: str  # "boundary" | "reference"


@dataclass
class Pack:
    animation: Animation
    images: list[RefImage] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)


def clock(stamp: str) -> str:
    """The cut sheet writes times as SS:CC (seconds:hundredths), so '05:00' is 5 seconds -> '5s'."""
    seconds, hundredths = stamp.split(":")
    value = int(seconds) + int(hundredths) / 100
    return f"{value:g}s"


def parse_animation(folder: Path) -> Animation:
    text = (folder / "animation.md").read_text(encoding="utf-8")

    title_line = re.search(r"^# Animation \d+ — (.+)$", text, re.M)
    # "## Chapter 4 — Package Management" in the early files, "## chapter_01 — The Absurdity ..." from 19 on.
    chapter = re.search(r"^## ([Cc]hapter\S*(?: \d+)? — .+)$", text, re.M)
    sequence = re.search(r"## Source animation sequence\n\n(.*?)\n\n## Sequence lock", text, re.S)
    palette = re.search(r"\*\*Palette:\*\*\s*(.+)", text)
    duration = re.search(r"\*\*Total duration:\*\*\s*(\d+) seconds \((\d+) ×", text)
    cuts = re.findall(r"## Cut (\d+) — (\d\d:\d\d)–(\d\d:\d\d)\n\n(.*?)\n\n### Ready-to-paste", text, re.S)
    if not (title_line and chapter and sequence and palette and duration and cuts):
        raise ValueError(f"{folder.name}: animation.md is missing a required section")
    if len(cuts) != int(duration.group(2)):
        raise ValueError(f"{folder.name}: {len(cuts)} cuts parsed but the file declares {duration.group(2)}")

    script = sequence.group(1).strip()
    note = None
    split = re.search(r"\n\n(\*\*Editor's note:\*\*.*)$", script, re.S)
    if split:
        note = split.group(1).strip()
        script = script[: split.start()].strip()

    if folder.name in BACKGROUND_OVERRIDES:
        bg_name, bg_hex, bg_note = BACKGROUND_OVERRIDES[folder.name]
    else:
        prompt = (folder / "clips/01/prompt.md").read_text(encoding="utf-8")
        bg = re.search(r"single background ([^`]+?) `(#[0-9A-Fa-f]{6})`", prompt)
        if not bg:
            raise ValueError(f"{folder.name}: no background colour in clips/01/prompt.md")
        bg_name, bg_hex, bg_note = bg.group(1).strip(), bg.group(2).upper(), None

    return Animation(
        folder=folder.name,
        title=title_line.group(1).replace("`", "").strip(),
        chapter=chapter.group(1).strip(),
        script=script,
        editors_note=note,
        palette=palette.group(1).strip(),
        seconds=int(duration.group(1)),
        cuts=[Cut(int(n), clock(a), clock(b), t.strip()) for n, a, b, t in cuts],
        bg_name=bg_name,
        bg_hex=bg_hex,
        bg_note=bg_note,
    )


def md5(path: Path) -> str:
    return hashlib.md5(path.read_bytes()).hexdigest()


def collect_images(anim: Animation) -> tuple[list[RefImage], list[str]]:
    """Boundary frames if any exist (they are the exact compositions); otherwise the other reference art."""
    notes: list[str] = []
    folder = CANONICAL / anim.folder
    expected = len(anim.cuts) + 1

    frames = sorted(DOCS.glob(f"{anim.folder}_frame_[0-9][0-9].png"))
    if frames:
        by_index = {int(re.search(r"_frame_(\d\d)\.png$", f.name).group(1)): f for f in frames}
        images: list[RefImage] = []
        missing = [i for i in range(expected) if i not in by_index]
        for index in sorted(by_index):
            if index == 0:
                role = "OPENING FRAME: the exact composition at 0s. Begin exactly on it."
            elif index == expected - 1:
                role = f"CLOSING FRAME: the exact composition at {anim.cuts[-1].end}. Finish exactly on it and hold."
            else:
                role = f"the exact composition reached at {anim.cuts[index - 1].end} (the end of beat {index} and the start of beat {index + 1})."
            images.append(RefImage(by_index[index], f"{len(images) + 1:02d}_frame_{index:02d}_at_{anim.cuts[index - 1].end if index else '0s'}.png", role, "boundary"))
        if missing:
            notes.append(
                "Boundary frame(s) missing: "
                + ", ".join(f"frame_{i:02d}" for i in missing)
                + ". Beats next to a missing frame are described in words only."
            )
        return images, notes

    seen: dict[str, Path] = {}
    for path in sorted(folder.glob("clips/*/references/*")):
        if path.suffix.lower() in IMAGE_SUFFIXES:
            seen.setdefault(md5(path), path)
    if not seen:
        return [], notes

    ordered = sorted(
        seen.values(),
        key=lambda p: (REFERENCE_ORDER.index(p.name) if p.name in REFERENCE_ORDER else len(REFERENCE_ORDER), p.name),
    )
    images = []
    for position, path in enumerate(ordered, start=1):
        what = REFERENCE_NOTES.get(path.name, GENERIC_ROLE)
        images.append(RefImage(
            path,
            f"{position:02d}_{path.name}",
            f"design reference, NOT an exact frame: {what}.",
            "reference",
        ))
    return images, notes


def build_prompt(pack: Pack) -> str:
    a = pack.animation
    header_note = ""
    if any(re.search(r"header", c.text, re.I) for c in a.cuts):
        header_note = " (A \"header\" named in the scene description is an object inside the story, not an app header.)"

    parts = [
        f"{a.title.upper()} — one continuous {a.seconds}-second animation in {len(a.cuts)} consecutive beats.",
        "",
        FORMAT_BLOCK.format(
            seconds=a.seconds, beats=len(a.cuts), bg_name=a.bg_name, bg_hex=a.bg_hex, header_note=header_note
        ),
        "",
        STYLE_BLOCK.format(palette=a.palette),
        "",
    ]

    if pack.images:
        parts.append("REFERENCE IMAGES (uploaded in this order)")
        boundary = pack.images[0].kind == "boundary"
        for number, image in enumerate(pack.images, start=1):
            parts.append(f"- @Image{number}: {image.role}")
        if boundary:
            parts.append("- Every character, prop, colour and scale in the animation must match these images exactly. Animate the shortest clear transformation from each image to the next.")
        else:
            parts.append("- Match these images for character, prop and colour design. They are not frames to copy; follow the scene description for what happens and when.")
        if any("model sheet" in i.role.lower() for i in pack.images):
            parts.append("- Never paint any text label from the model sheet (such as USER_A or FOREMAN_A) into the animation.")
        parts.append("")
    else:
        parts += [
            "REFERENCE IMAGES",
            "- None. No approved artwork exists for this animation yet. Design each recurring character and prop once, keep it identical throughout, and use the palette and flat background above.",
            "",
        ]

    parts.append("SCENE DESCRIPTION (complete, in order)")
    for cut in a.cuts:
        parts.append(f"Beat {cut.number} of {len(a.cuts)} — {cut.start}–{cut.end}")
        parts.append(cut.text)
        parts.append("")

    parts.append("COMPLETE SCRIPT (verbatim from the Guide)")
    # The Guide marks spoken lines in bold; the markers are formatting, not words, so they are dropped.
    parts.append(a.script.replace("**", ""))
    if a.editors_note:
        parts += [
            "",
            "Editor's note (context only; do not show it, write it on screen or speak it):",
            a.editors_note.replace("**Editor's note:**", "").strip(),
        ]
    return "\n".join(parts).rstrip() + "\n"


def build_readme(pack: Pack, prompt_chars: int) -> str:
    a = pack.animation
    lines = [
        f"{a.title} — Dreamina Seedance 2.5 pack",
        f"{a.chapter}",
        "",
        "SETTINGS",
        f"  Aspect ratio: 1:1        Duration: {a.seconds} seconds        One generation for the whole animation.",
        f"  Background:   {a.bg_name} {a.bg_hex}" + (f"  ({a.bg_note})" if a.bg_note else ""),
        f"  Prompt length: {prompt_chars} characters. Seedance's prompt-length limit is not stated in the sources I found; if",
        "  it rejects the prompt, trim the VISUAL DESIGN section first and keep the scene description and script intact.",
        "",
        "HOW TO USE",
        "  1. Upload everything in references/ in file-name order, so @Image1, @Image2 ... match the prompt.",
        "  2. Paste prompt.txt as the prompt.",
    ]
    if pack.images and pack.images[0].kind == "boundary":
        last = len(pack.images)
        lines += [
            "  3. The images are exact boundary compositions. If you use a first-frame / last-frame mode instead of reference",
            f"     mode, use the first image as the opening frame and image {last} as the last frame. I could not confirm that",
            "     Seedance 2.5 allows first/last-frame mode and reference images in the same generation.",
        ]
    lines += ["", "REFERENCE IMAGES"]
    if pack.images:
        for number, image in enumerate(pack.images, start=1):
            lines.append(f"  @Image{number}  references/{image.zip_name}")
            lines.append(f"      {image.role}")
    else:
        lines.append("  None exist for this animation yet. The prompt tells Seedance to design the characters itself.")
        lines.append("  Project rule: generate and approve frame_00 first, then build the rest from it, so one character")
        lines.append("  design carries through. Re-run scripts/build_seedance_packs.py once frames are added to docs/.")

    flags = list(pack.notes)
    if a.bg_note:
        flags.append(
            f"Background: other repo prompt files say a different colour for this entry; the reference images are {a.bg_name}, "
            "and their navy figures would vanish on navy, so this pack follows the images."
        )
    if any("model sheet" in i.role.lower() for i in pack.images):
        flags.append(
            "The model sheet carries production labels (USER_A, FOREMAN_A ...). The repo's own rules say such labels must never "
            "reach the animation, so the prompt forbids painting them. If Seedance still draws them, remove that image."
        )
    if any(re.search(r"header", c.text, re.I) for c in a.cuts):
        flags.append("The scene text says \"header\" for an object in the story (not an app header); the prompt says so.")
    if flags:
        lines += ["", "THINGS TO KNOW"]
        lines += [f"  - {flag}" for flag in flags]
    lines += [
        "",
        "SOURCES",
        f"  docs/guide/animations/canonical/{a.folder}/animation.md and clips/*/prompt.md; docs/{a.folder}_frame_*.png where present.",
    ]
    return "\n".join(lines) + "\n"


def write_zip(pack: Pack, out_dir: Path) -> tuple[Path, int]:
    a = pack.animation
    prompt = build_prompt(pack)
    readme = build_readme(pack, len(prompt))
    target = out_dir / f"{a.folder}_seedance2.5.zip"
    root = f"{a.folder}_seedance2.5"

    def add(zf: zipfile.ZipFile, name: str, data: bytes, compress: int) -> None:
        info = zipfile.ZipInfo(f"{root}/{name}", ZIP_TIME)
        info.compress_type = compress
        info.external_attr = 0o644 << 16
        zf.writestr(info, data)

    with zipfile.ZipFile(target, "w") as zf:
        add(zf, "prompt.txt", prompt.encode("utf-8"), zipfile.ZIP_DEFLATED)
        add(zf, "README.txt", readme.encode("utf-8"), zipfile.ZIP_DEFLATED)
        for image in pack.images:
            add(zf, f"references/{image.zip_name}", image.source.read_bytes(), zipfile.ZIP_STORED)
    return target, len(prompt)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", required=True, type=Path, help="directory to write the zips and INDEX.txt into")
    parser.add_argument("--only", action="append", default=[], help="build only this folder (e.g. 02_pkg_install); repeatable")
    args = parser.parse_args(argv)

    folders = sorted(p for p in CANONICAL.iterdir() if (p / "animation.md").is_file())
    if args.only:
        folders = [p for p in folders if p.name in set(args.only)]
        if not folders:
            print("no matching animation folders", file=sys.stderr)
            return 2

    args.out.mkdir(parents=True, exist_ok=True)
    index = ["Dreamina Seedance 2.5 packs — one zip per animation, whole animation in one generation, 1:1, no header.", ""]
    for folder in folders:
        anim = parse_animation(folder)
        images, notes = collect_images(anim)
        pack = Pack(anim, images, notes)
        path, chars = write_zip(pack, args.out)
        kind = "none" if not images else f"{len(images)} {images[0].kind} image(s)"
        index.append(f"{path.name:44} {anim.seconds:>2}s  {len(anim.cuts)} beats  {chars:>5} chars  refs: {kind}")
        print(index[-1])
    (args.out / "INDEX.txt").write_text("\n".join(index) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
