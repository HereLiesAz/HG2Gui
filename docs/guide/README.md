# HG2Gui Guide material

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

Canonical START/END filenames come directly from `HG2Gui_animation_5s_cut_sheet_v2.md`. If a declared image does not actually exist in the repository, its `frames/README.md`, the animation `storyboard.md`, and `MANIFEST.json` record the absence. No replacement frame is invented.

Tangent sources provide one ordered still per cut rather than explicit START/END pairs. Those stills are treated as cut-end boundaries and the previous still is reused as the following cut's inferred start. The first tangent cut's start remains explicitly unresolved where no preceding still exists.

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

- Canonical: **8 / 234** boundary slots materialized; **226** missing; **0** ambiguous; **0** unresolved.
- Tangents: **80 / 84** boundary slots materialized; **0** missing; **0** ambiguous; **4** unresolved.
- Legacy: **28 / 28** boundary slots materialized; **0** missing; **0** ambiguous; **0** unresolved.

`MANIFEST.json` records provenance, source semantics, and exact storyboard coverage.
