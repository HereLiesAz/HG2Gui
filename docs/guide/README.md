# HG2Gui Guide material

This is the organized, non-destructive collection of Guide-related production material. Source files remain in their original locations so application and documentation paths do not break.

## Structure

- `project/` — Guide-wide prose, design/motion references, shared production rules, relevant source-code copies, and unresolved references.
- `animations/canonical/` — the 35 current technical 5-second production-packet entries.
- `animations/tangents/` — the four scripted Guide tangents that precede canonical entries.
- `animations/legacy/` — older/incomplete animated material that has numbered visual material but no current production packet.

Each animation preserves its existing source material and storyboard, with a [production clip index](CLIP_PRODUCTION_INDEX.md). Each `clips/NN/` contains independently sourced START and END frames when available, its source-matched script passage, separate START/END shot descriptions, and an individualized image-to-video prompt. Historical `scenes/NN/` source records are retained.

## Clip production

[Browse all animation clip packets](CLIP_PRODUCTION_INDEX.md). Use each animation's `CLIPS.md` index to find a clip's `README.md`, `script.md`, `shots.md`, `prompt.md`, and available START/END image files. Missing frames and legacy narration remain explicitly unresolved, not invented. Run `scripts/organize_guide_material.py` to refresh source inventories without erasing curated clip writing.

## Visual material

Original labeled tangent screenshots are preserved. Missing clean frames are documented in `ANIMATION_TODO.md`; they are not invented.

The three visual classes are deliberately separate:

1. `scenes/NN/frames/` — storyboard boundary frames. `start.*` and `end.*` are normalized copies of real repository assets when those assets exist.
2. `scenes/NN/references/` — supporting visual material specific to that scene, excluding storyboard boundaries.
3. `project/references/shared/` — Guide-wide style and design references.

Adjacent scenes may intentionally contain duplicate copies of the same boundary image: one scene's end is the next scene's start. Duplication here is structural, not accidental.

Canonical START/END filenames come directly from `HG2Gui_animation_5s_cut_sheet_v2.md`. If a declared image does not actually exist in the repository, its `frames/README.md`, the animation `storyboard.md`, and `MANIFEST.json` record the absence. No replacement frame is invented.

Tangent cut-map PNGs visibly include a `Start:` overlay. Each is stored as that cut's labeled START source screenshot, not as its END or the next clip's START. All 42 tangent END images remain unverified; the annotated sources need clean artwork extraction.

Legacy `ls`, `echo`, and `ls_tangent` clip sets already provide explicit `_start` and `_end` files per scene; those are copied directly into each scene's `frames/` folder.

## Inventory

- Canonical animations: **35**
- Canonical scenes: **117**
- Tangent animations: **4**
- Tangent scenes: **42**
- Legacy/incomplete animation groups: **3**
- Legacy scenes: **14**
- Project-wide files copied: **63**
- Unassigned scene/reference media retained for review: **0**

## Storyboard boundary coverage

- Canonical: **10 / 234** source-image positions; **224** missing; **0** ambiguous; **0** unresolved.
- Tangents: **42 / 84** labeled START screenshots available; **42** END images unresolved.
- Legacy: **28 / 28** explicit source image positions; written source narration absent.

`MANIFEST.json` records provenance, source semantics, and exact storyboard coverage.
