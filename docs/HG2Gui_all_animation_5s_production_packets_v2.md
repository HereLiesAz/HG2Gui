# HG2Gui — 1:1 Five-Second Guide Animation Production Packets v2

These packets remove all Guide UI from the generated video. HG2Gui supplies titles, tabs, borders and framing itself. The generated animation is only the square inner picture: one flat background color plus the animation artwork.

## Character consistency protocol

Each entry is now treated as a tiny production with its own canonical cast:

1. Generate the **Sequence Reference Board** once from the packet's character lock.
2. Generate the entry's **boundary frames** from that same board. An entry with N five-second cuts needs only N+1 unique boundary images.
3. Reuse each boundary image literally: `frame_01` is Cut 1's END and Cut 2's START. Do not regenerate it.
4. Generate every five-second video cut with three visual references whenever the tool supports them: **Sequence Reference Board + exact START frame + exact END frame**.
5. Keep model version/seed fixed within an entry when the tool exposes those controls. The reference images, however, are the source of truth.

This converts character continuity from a request into an asset pipeline. The model is no longer asked to remember who somebody was five seconds ago, a task for which it has repeatedly demonstrated the moral seriousness of a goldfish.


---

# Animation 01 — `pwd` — print working directory

## Chapter 3 — Navigation

## Source animation sequence

A user runs `pwd`.

The pathname appears.

A tiny picket line immediately forms around the directory:

`WORKING DIRECTORY — WORKING WAGES`

The user points out that *they* are the one doing the work.

The directories confer.

A new placard appears:

`THEN STOP WORKING IN US`

The user types `cd ..`.

The entire picket line follows.

**Editor's note:** The Directory Workers' Union later denied following anyone. They insist they were already there and that `pwd` can prove it. `pwd` has declined to become involved in another tribunal.

## Sequence lock

**Background:** midnight navy `#101D2D`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

USER_A: angular brick-red human pictogram, rectangular head, narrow torso, long straight limbs; DIRECTORY_WORKER_BASE: squat bottle-green worker pictogram with rounded hardhat, rectangular torso, short legs; all union workers are exact clones of DIRECTORY_WORKER_BASE. Keep these silhouettes unchanged in every frame.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
USER_A: angular brick-red human pictogram, rectangular head, narrow torso, long straight limbs; DIRECTORY_WORKER_BASE: squat bottle-green worker pictogram with rounded hardhat, rectangular torso, short legs; all union workers are exact clones of DIRECTORY_WORKER_BASE. Keep these silhouettes unchanged in every frame.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `01_pwd_frame_00.png`

**Used as:** Cut 01 START

Locked wide pictogram of a filesystem corridor. At 0.5s a tiny user taps a `pwd` capsule; the pathname draws itself as a clean horizontal route label.

### Frame 01 — `01_pwd_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

The user noticing the picket line.

### Frame 02 — `01_pwd_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

One worker emerging holding a blank replacement placard.

### Frame 03 — `01_pwd_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

The whole line moving toward frame edge.

### Frame 04 — `01_pwd_frame_04.png`

**Used as:** Cut 04 END

The deadpan tableau for the cut.

## Cut 01 — 00:00–05:00

**START:** `01_pwd_frame_00.png`

**END:** `01_pwd_frame_01.png`

Locked wide pictogram of a filesystem corridor. At 0.5s a tiny user taps a `pwd` capsule; the pathname draws itself as a clean horizontal route label. From the directory blocks, miniature workers pop up and silently plant `WORKING DIRECTORY — WORKING WAGES` placards. End on the user noticing the picket line.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight navy #101D2D
Character/prop lock: USER_A: angular brick-red human pictogram, rectangular head, narrow torso, long straight limbs; DIRECTORY_WORKER_BASE: squat bottle-green worker pictogram with rounded hardhat, rectangular torso, short legs; all union workers are exact clones of DIRECTORY_WORKER_BASE. Keep these silhouettes unchanged in every frame.
Reference board: use the canonical sequence reference board for Animation 01.
START FRAME: 01_pwd_frame_00.png
END FRAME: 01_pwd_frame_01.png

CUT 01 OF 04 — 00:00–05:00
Locked wide pictogram of a filesystem corridor. At 0.5s a tiny user taps a `pwd` capsule; the pathname draws itself as a clean horizontal route label. From the directory blocks, miniature workers pop up and silently plant `WORKING DIRECTORY — WORKING WAGES` placards. End on the user noticing the picket line.
```

## Cut 02 — 05:00–10:00

**START:** `01_pwd_frame_01.png`

**END:** `01_pwd_frame_02.png`

Continue the exact frame. The user points at themself, then at the terminal, clearly arguing that the user is doing the work. The directory workers huddle in a tiny circular conference, placards tucked under arms. End with one worker emerging holding a blank replacement placard.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight navy #101D2D
Character/prop lock: USER_A: angular brick-red human pictogram, rectangular head, narrow torso, long straight limbs; DIRECTORY_WORKER_BASE: squat bottle-green worker pictogram with rounded hardhat, rectangular torso, short legs; all union workers are exact clones of DIRECTORY_WORKER_BASE. Keep these silhouettes unchanged in every frame.
Reference board: use the canonical sequence reference board for Animation 01.
START FRAME: 01_pwd_frame_01.png
END FRAME: 01_pwd_frame_02.png

CUT 02 OF 04 — 05:00–10:00
Continue the exact frame. The user points at themself, then at the terminal, clearly arguing that the user is doing the work. The directory workers huddle in a tiny circular conference, placards tucked under arms. End with one worker emerging holding a blank replacement placard.
```

## Cut 03 — 10:00–15:00

**START:** `01_pwd_frame_02.png`

**END:** `01_pwd_frame_03.png`

The worker flips the new placard toward camera: `THEN STOP WORKING IN US`. The user stares, then types `cd ..`; the visible path steps upward one level like a diagram. The picket line instantly pivots and begins marching after the user. End with the whole line moving toward frame edge.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight navy #101D2D
Character/prop lock: USER_A: angular brick-red human pictogram, rectangular head, narrow torso, long straight limbs; DIRECTORY_WORKER_BASE: squat bottle-green worker pictogram with rounded hardhat, rectangular torso, short legs; all union workers are exact clones of DIRECTORY_WORKER_BASE. Keep these silhouettes unchanged in every frame.
Reference board: use the canonical sequence reference board for Animation 01.
START FRAME: 01_pwd_frame_02.png
END FRAME: 01_pwd_frame_03.png

CUT 03 OF 04 — 10:00–15:00
The worker flips the new placard toward camera: `THEN STOP WORKING IN US`. The user stares, then types `cd ..`; the visible path steps upward one level like a diagram. The picket line instantly pivots and begins marching after the user. End with the whole line moving toward frame edge.
```

## Cut 04 — 15:00–20:00

**START:** `01_pwd_frame_03.png`

**END:** `01_pwd_frame_04.png`

The user arrives one directory up. Before they can relax, the identical picket line slides in behind and resumes formation as if it had always been there. A tiny official stamp appears: `UNION POSITION: WE WERE ALREADY HERE`. Hold the deadpan tableau for the cut.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight navy #101D2D
Character/prop lock: USER_A: angular brick-red human pictogram, rectangular head, narrow torso, long straight limbs; DIRECTORY_WORKER_BASE: squat bottle-green worker pictogram with rounded hardhat, rectangular torso, short legs; all union workers are exact clones of DIRECTORY_WORKER_BASE. Keep these silhouettes unchanged in every frame.
Reference board: use the canonical sequence reference board for Animation 01.
START FRAME: 01_pwd_frame_03.png
END FRAME: 01_pwd_frame_04.png

CUT 04 OF 04 — 15:00–20:00
The user arrives one directory up. Before they can relax, the identical picket line slides in behind and resumes formation as if it had always been there. A tiny official stamp appears: `UNION POSITION: WE WERE ALREADY HERE`. Hold the deadpan tableau for the cut.
```


---

# Animation 02 — `pkg install` — package install

## Chapter 4 — Package Management

## Source animation sequence

A small package sits in a chair.

Several concerned packages sit around it.

A counsellor asks:

**"Do you feel you rely too heavily on libfoo?"**

The package nods.

The counsellor turns to the door.

**"Bring in libfoo."**

libfoo enters with three dependencies.

Those dependencies enter with nine more.

The room fills.

The walls bulge.

A forklift arrives carrying another room.

Terminal:

`Installing dependencies...`

The original package smiles for the first time.

## Sequence lock

**Background:** oxblood `#7A1E2D`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

PACKAGE_A: small square parcel-person with one seam line and two stick limbs; COUNSELLOR_A: tall narrow humanoid with clipboard-shaped torso; DEPENDENCY_BASE: smaller parcel-person cloned identically for every dependency except size may step down by 15% per generation. Never redesign PACKAGE_A or COUNSELLOR_A.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
PACKAGE_A: small square parcel-person with one seam line and two stick limbs; COUNSELLOR_A: tall narrow humanoid with clipboard-shaped torso; DEPENDENCY_BASE: smaller parcel-person cloned identically for every dependency except size may step down by 15% per generation. Never redesign PACKAGE_A or COUNSELLOR_A.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `02_pkg_install_frame_00.png`

**Used as:** Cut 01 START

A therapy-room diagram: one small software package sits in a chair, three concerned packages around it.

### Frame 01 — `02_pkg_install_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Counsellor turning toward a closed door.

### Frame 02 — `02_pkg_install_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

The counsellor realizes the chain continues offscreen.

### Frame 03 — `02_pkg_install_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

The room completely full and one final dependency knocking from outside.

### Frame 04 — `02_pkg_install_frame_04.png`

**Used as:** Cut 04 END

The absurdly successful intervention.

## Cut 01 — 00:00–05:00

**START:** `02_pkg_install_frame_00.png`

**END:** `02_pkg_install_frame_01.png`

A therapy-room diagram: one small software package sits in a chair, three concerned packages around it. A counsellor displays a card reading `DEPENDENCY`. The counsellor asks with a simple caption bubble: `TOO RELIANT ON LIBFOO?` The package nods. End with counsellor turning toward a closed door.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: oxblood #7A1E2D
Character/prop lock: PACKAGE_A: small square parcel-person with one seam line and two stick limbs; COUNSELLOR_A: tall narrow humanoid with clipboard-shaped torso; DEPENDENCY_BASE: smaller parcel-person cloned identically for every dependency except size may step down by 15% per generation. Never redesign PACKAGE_A or COUNSELLOR_A.
Reference board: use the canonical sequence reference board for Animation 02.
START FRAME: 02_pkg_install_frame_00.png
END FRAME: 02_pkg_install_frame_01.png

CUT 01 OF 04 — 00:00–05:00
A therapy-room diagram: one small software package sits in a chair, three concerned packages around it. A counsellor displays a card reading `DEPENDENCY`. The counsellor asks with a simple caption bubble: `TOO RELIANT ON LIBFOO?` The package nods. End with counsellor turning toward a closed door.
```

## Cut 02 — 05:00–10:00

**START:** `02_pkg_install_frame_01.png`

**END:** `02_pkg_install_frame_02.png`

Continue. Door swings open. A capsule label reads `BRING IN LIBFOO`. `libfoo` enters, immediately followed by three smaller dependencies attached in a little chain. The original package brightens slightly. End as the counsellor realizes the chain continues offscreen.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: oxblood #7A1E2D
Character/prop lock: PACKAGE_A: small square parcel-person with one seam line and two stick limbs; COUNSELLOR_A: tall narrow humanoid with clipboard-shaped torso; DEPENDENCY_BASE: smaller parcel-person cloned identically for every dependency except size may step down by 15% per generation. Never redesign PACKAGE_A or COUNSELLOR_A.
Reference board: use the canonical sequence reference board for Animation 02.
START FRAME: 02_pkg_install_frame_01.png
END FRAME: 02_pkg_install_frame_02.png

CUT 02 OF 04 — 05:00–10:00
Continue. Door swings open. A capsule label reads `BRING IN LIBFOO`. `libfoo` enters, immediately followed by three smaller dependencies attached in a little chain. The original package brightens slightly. End as the counsellor realizes the chain continues offscreen.
```

## Cut 03 — 10:00–15:00

**START:** `02_pkg_install_frame_02.png`

**END:** `02_pkg_install_frame_03.png`

The chain keeps arriving: three become nine, nine become a dense crowd. Packages slide in linearly from every doorway and stack into the room until the walls visibly bow outward like a diagram under pressure. End with the room completely full and one final dependency knocking from outside.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: oxblood #7A1E2D
Character/prop lock: PACKAGE_A: small square parcel-person with one seam line and two stick limbs; COUNSELLOR_A: tall narrow humanoid with clipboard-shaped torso; DEPENDENCY_BASE: smaller parcel-person cloned identically for every dependency except size may step down by 15% per generation. Never redesign PACKAGE_A or COUNSELLOR_A.
Reference board: use the canonical sequence reference board for Animation 02.
START FRAME: 02_pkg_install_frame_02.png
END FRAME: 02_pkg_install_frame_03.png

CUT 03 OF 04 — 10:00–15:00
The chain keeps arriving: three become nine, nine become a dense crowd. Packages slide in linearly from every doorway and stack into the room until the walls visibly bow outward like a diagram under pressure. End with the room completely full and one final dependency knocking from outside.
```

## Cut 04 — 15:00–20:00

**START:** `02_pkg_install_frame_03.png`

**END:** `02_pkg_install_frame_04.png`

A forklift enters carrying an entire second room as cargo and docks it to the first. A terminal strip at the bottom prints `Installing dependencies...`. The original package finally smiles while everyone else is crushed into geometric order. Hold on the absurdly successful intervention.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: oxblood #7A1E2D
Character/prop lock: PACKAGE_A: small square parcel-person with one seam line and two stick limbs; COUNSELLOR_A: tall narrow humanoid with clipboard-shaped torso; DEPENDENCY_BASE: smaller parcel-person cloned identically for every dependency except size may step down by 15% per generation. Never redesign PACKAGE_A or COUNSELLOR_A.
Reference board: use the canonical sequence reference board for Animation 02.
START FRAME: 02_pkg_install_frame_03.png
END FRAME: 02_pkg_install_frame_04.png

CUT 04 OF 04 — 15:00–20:00
A forklift enters carrying an entire second room as cargo and docks it to the first. A terminal strip at the bottom prints `Installing dependencies...`. The original package finally smiles while everyone else is crushed into geometric order. Hold on the absurdly successful intervention.
```


---

# Animation 03 — `apt-get update` — refresh package indexes

## Chapter 4 — Package Management

## Source animation sequence

An elderly alien sits at a government desk.

A clerk stamps his birth certificate:

**AGE: 4**

The alien looks at his hands.

Still elderly.

The clerk stamps harder.

Nothing changes.

Cut to a terminal running:

`apt-get update`

A package list changes from `1.2` to `1.3`.

The installed package beside it remains `1.2`.

The clerk nods approvingly.

**"There. Much better."**

## Sequence lock

**Background:** powder blue `#7EA6B7`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

ALIEN_A: elderly pear-shaped alien with long narrow forearms and one drooping shoulder; CLERK_A: rigid rectangular bureaucrat with small round head and stamp arm. Preserve exact body proportions and face marks.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
ALIEN_A: elderly pear-shaped alien with long narrow forearms and one drooping shoulder; CLERK_A: rigid rectangular bureaucrat with small round head and stamp arm. Preserve exact body proportions and face marks.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `03_apt_get_update_frame_00.png`

**Used as:** Cut 01 START

Government-office tableau. An elderly alien sits at a desk.

### Frame 01 — `03_apt_get_update_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

The clerk reaching for an even larger stamp.

### Frame 02 — `03_apt_get_update_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Old installed block unchanged.

### Frame 03 — `03_apt_get_update_frame_03.png`

**Used as:** Cut 03 END

Dryly.

## Cut 01 — 00:00–05:00

**START:** `03_apt_get_update_frame_00.png`

**END:** `03_apt_get_update_frame_01.png`

Government-office tableau. An elderly alien sits at a desk. A clerk calmly stamps a birth certificate `AGE: 4`. The alien lifts one ancient hand, looks from the hand to the certificate. The clerk stamps the paper again, harder. Nothing about the alien changes. End on the clerk reaching for an even larger stamp.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: powder blue #7EA6B7
Character/prop lock: ALIEN_A: elderly pear-shaped alien with long narrow forearms and one drooping shoulder; CLERK_A: rigid rectangular bureaucrat with small round head and stamp arm. Preserve exact body proportions and face marks.
Reference board: use the canonical sequence reference board for Animation 03.
START FRAME: 03_apt_get_update_frame_00.png
END FRAME: 03_apt_get_update_frame_01.png

CUT 01 OF 03 — 00:00–05:00
Government-office tableau. An elderly alien sits at a desk. A clerk calmly stamps a birth certificate `AGE: 4`. The alien lifts one ancient hand, looks from the hand to the certificate. The clerk stamps the paper again, harder. Nothing about the alien changes. End on the clerk reaching for an even larger stamp.
```

## Cut 02 — 05:00–10:00

**START:** `03_apt_get_update_frame_01.png`

**END:** `03_apt_get_update_frame_02.png`

Cut within same palette to a stripped-down terminal diagram: `apt-get update`. A package-index card labeled `1.2` flips to `1.3`; beside it an installed-package block stays visibly `1.2`. Repeat once with another index card to make the distinction unmistakable. End with old installed block unchanged.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: powder blue #7EA6B7
Character/prop lock: ALIEN_A: elderly pear-shaped alien with long narrow forearms and one drooping shoulder; CLERK_A: rigid rectangular bureaucrat with small round head and stamp arm. Preserve exact body proportions and face marks.
Reference board: use the canonical sequence reference board for Animation 03.
START FRAME: 03_apt_get_update_frame_01.png
END FRAME: 03_apt_get_update_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Cut within same palette to a stripped-down terminal diagram: `apt-get update`. A package-index card labeled `1.2` flips to `1.3`; beside it an installed-package block stays visibly `1.2`. Repeat once with another index card to make the distinction unmistakable. End with old installed block unchanged.
```

## Cut 03 — 10:00–15:00

**START:** `03_apt_get_update_frame_02.png`

**END:** `03_apt_get_update_frame_03.png`

Return to clerk and alien in split-screen with the package diagram. Clerk nods with bureaucratic satisfaction and places a neat `THERE. MUCH BETTER.` label beneath the certificate while the alien and installed package remain exactly as old as before. Hold dryly.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: powder blue #7EA6B7
Character/prop lock: ALIEN_A: elderly pear-shaped alien with long narrow forearms and one drooping shoulder; CLERK_A: rigid rectangular bureaucrat with small round head and stamp arm. Preserve exact body proportions and face marks.
Reference board: use the canonical sequence reference board for Animation 03.
START FRAME: 03_apt_get_update_frame_02.png
END FRAME: 03_apt_get_update_frame_03.png

CUT 03 OF 03 — 10:00–15:00
Return to clerk and alien in split-screen with the package diagram. Clerk nods with bureaucratic satisfaction and places a neat `THERE. MUCH BETTER.` label beneath the certificate while the alien and installed package remain exactly as old as before. Hold dryly.
```


---

# Animation 04 — `nano` — Nano's ANOther editor

## Chapter 6 — Text Editors

## Source animation sequence

The word `nano` appears.

It opens.

Inside is another smaller `nano`.

That opens.

Inside is another.

And another.

The camera keeps zooming inward until the letters become microscopic.

A tiny footer appears at the bottom:

`^X Exit`

The camera zooms another ten levels to make it readable.

A large human finger enters frame, misses the tiny `^X`, and presses the entire universe instead.

## Sequence lock

**Background:** deep cobalt `#183D7A`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

NANO_TILE: the same rounded rectangular word-object repeated recursively at exact proportional scale; FINGER_A: one oversized simplified hand silhouette with a square fingertip. No humanoid redesign occurs between cuts.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
NANO_TILE: the same rounded rectangular word-object repeated recursively at exact proportional scale; FINGER_A: one oversized simplified hand silhouette with a square fingertip. No humanoid redesign occurs between cuts.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `04_nano_frame_00.png`

**Used as:** Cut 01 START

The single word `nano` sits centered like a labeled object. It unfolds like a hinged capsule, revealing a smaller `nano` inside.

### Frame 01 — `04_nano_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

End mid-zoom with three visible nested layers.

### Frame 02 — `04_nano_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

The footer a speck.

### Frame 03 — `04_nano_frame_03.png`

**Used as:** Cut 03 END

Hard hold on the squashed cosmos.

## Cut 01 — 00:00–05:00

**START:** `04_nano_frame_00.png`

**END:** `04_nano_frame_01.png`

The single word `nano` sits centered like a labeled object. It unfolds like a hinged capsule, revealing a smaller `nano` inside. That one unfolds to reveal another, then another, each nested cleanly with no 3D shading. Begin a steady 2D zoom inward. End mid-zoom with three visible nested layers.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: deep cobalt #183D7A
Character/prop lock: NANO_TILE: the same rounded rectangular word-object repeated recursively at exact proportional scale; FINGER_A: one oversized simplified hand silhouette with a square fingertip. No humanoid redesign occurs between cuts.
Reference board: use the canonical sequence reference board for Animation 04.
START FRAME: 04_nano_frame_00.png
END FRAME: 04_nano_frame_01.png

CUT 01 OF 03 — 00:00–05:00
The single word `nano` sits centered like a labeled object. It unfolds like a hinged capsule, revealing a smaller `nano` inside. That one unfolds to reveal another, then another, each nested cleanly with no 3D shading. Begin a steady 2D zoom inward. End mid-zoom with three visible nested layers.
```

## Cut 02 — 05:00–10:00

**START:** `04_nano_frame_01.png`

**END:** `04_nano_frame_02.png`

Continue the same zoom with identical nested geometry. More `nano` layers open in rapid but linear succession until the word becomes microscopic. A tiny footer appears along the bottom edge: `^X Exit`. The camera overshoots past it, making the footer nearly unreadable. End with the footer a speck.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: deep cobalt #183D7A
Character/prop lock: NANO_TILE: the same rounded rectangular word-object repeated recursively at exact proportional scale; FINGER_A: one oversized simplified hand silhouette with a square fingertip. No humanoid redesign occurs between cuts.
Reference board: use the canonical sequence reference board for Animation 04.
START FRAME: 04_nano_frame_01.png
END FRAME: 04_nano_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Continue the same zoom with identical nested geometry. More `nano` layers open in rapid but linear succession until the word becomes microscopic. A tiny footer appears along the bottom edge: `^X Exit`. The camera overshoots past it, making the footer nearly unreadable. End with the footer a speck.
```

## Cut 03 — 10:00–15:00

**START:** `04_nano_frame_02.png`

**END:** `04_nano_frame_03.png`

The camera performs an absurd further ten-level zoom to make `^X Exit` barely legible again. A gigantic simplified human finger enters from above, aims for the tiny exit mark, misses by a few pixels, and presses the entire nested universe flat like a button. Hard hold on the squashed cosmos.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: deep cobalt #183D7A
Character/prop lock: NANO_TILE: the same rounded rectangular word-object repeated recursively at exact proportional scale; FINGER_A: one oversized simplified hand silhouette with a square fingertip. No humanoid redesign occurs between cuts.
Reference board: use the canonical sequence reference board for Animation 04.
START FRAME: 04_nano_frame_02.png
END FRAME: 04_nano_frame_03.png

CUT 03 OF 03 — 10:00–15:00
The camera performs an absurd further ten-level zoom to make `^X Exit` barely legible again. A gigantic simplified human finger enters from above, aims for the tiny exit mark, misses by a few pixels, and presses the entire nested universe flat like a button. Hard hold on the squashed cosmos.
```


---

# Animation 05 — `top` — process monitor

## Chapter 7 — Processes

## Source animation sequence

A leaderboard appears beneath:

`SORT: %CPU`

1. sensible_process — 2%
2. useful_process — 1%
3. idle_process — 0%

A clipboard marked **PERFORMANCE REVIEW** enters frame.

The process at the bottom starts eating CPU.

Its percentage rises.

The others notice the clipboard.

Soon every process is shovelling CPU into itself while climbing the board.

One reaches 99%.

A little trophy appears.

The terminal freezes.

The Board records a record quarter.

## Sequence lock

**Background:** dark teal `#073D43`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

PROCESS_BASE: tiny rounded rectangular process-person with two short legs and one shovel arm; all competing processes are clones differentiated only by row label. TROPHY_A is a small geometric cup. Preserve clone geometry.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
PROCESS_BASE: tiny rounded rectangular process-person with two short legs and one shovel arm; all competing processes are clones differentiated only by row label. TROPHY_A is a small geometric cup. Preserve clone geometry.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `05_top_frame_00.png`

**Used as:** Cut 01 START

A flat process leaderboard appears under the header `SORT: %CPU`: `sensible_process 2%`, `useful_process 1%`, `idle_process 0%`.

### Frame 01 — `05_top_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

The bottom process staring at the clipboard.

### Frame 02 — `05_top_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

All rows racing upward in CPU use.

### Frame 03 — `05_top_frame_03.png`

**Used as:** Cut 03 END

The trophy beside a completely unusable system.

## Cut 01 — 00:00–05:00

**START:** `05_top_frame_00.png`

**END:** `05_top_frame_01.png`

A flat process leaderboard appears under the header `SORT: %CPU`: `sensible_process 2%`, `useful_process 1%`, `idle_process 0%`. A large clipboard labeled `PERFORMANCE REVIEW` slides in from frame right. The bottom process notices it. End on the bottom process staring at the clipboard.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark teal #073D43
Character/prop lock: PROCESS_BASE: tiny rounded rectangular process-person with two short legs and one shovel arm; all competing processes are clones differentiated only by row label. TROPHY_A is a small geometric cup. Preserve clone geometry.
Reference board: use the canonical sequence reference board for Animation 05.
START FRAME: 05_top_frame_00.png
END FRAME: 05_top_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A flat process leaderboard appears under the header `SORT: %CPU`: `sensible_process 2%`, `useful_process 1%`, `idle_process 0%`. A large clipboard labeled `PERFORMANCE REVIEW` slides in from frame right. The bottom process notices it. End on the bottom process staring at the clipboard.
```

## Cut 02 — 05:00–10:00

**START:** `05_top_frame_01.png`

**END:** `05_top_frame_02.png`

The bottom process begins literally shovelling little CPU blocks into itself; its percentage climbs and its row moves upward. The other processes notice and imitate it. The leaderboard becomes frantic but remains cleanly diagrammatic. End with all rows racing upward in CPU use.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark teal #073D43
Character/prop lock: PROCESS_BASE: tiny rounded rectangular process-person with two short legs and one shovel arm; all competing processes are clones differentiated only by row label. TROPHY_A is a small geometric cup. Preserve clone geometry.
Reference board: use the canonical sequence reference board for Animation 05.
START FRAME: 05_top_frame_01.png
END FRAME: 05_top_frame_02.png

CUT 02 OF 03 — 05:00–10:00
The bottom process begins literally shovelling little CPU blocks into itself; its percentage climbs and its row moves upward. The other processes notice and imitate it. The leaderboard becomes frantic but remains cleanly diagrammatic. End with all rows racing upward in CPU use.
```

## Cut 03 — 10:00–15:00

**START:** `05_top_frame_02.png`

**END:** `05_top_frame_03.png`

One process hits `99%` and receives a tiny trophy. Confetti appears for half a second; simultaneously the entire terminal freezes rigid. A boardroom stamp descends over the frozen picture: `RECORD QUARTER`. Hold on the trophy beside a completely unusable system.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark teal #073D43
Character/prop lock: PROCESS_BASE: tiny rounded rectangular process-person with two short legs and one shovel arm; all competing processes are clones differentiated only by row label. TROPHY_A is a small geometric cup. Preserve clone geometry.
Reference board: use the canonical sequence reference board for Animation 05.
START FRAME: 05_top_frame_02.png
END FRAME: 05_top_frame_03.png

CUT 03 OF 03 — 10:00–15:00
One process hits `99%` and receives a tiny trophy. Confetti appears for half a second; simultaneously the entire terminal freezes rigid. A boardroom stamp descends over the frozen picture: `RECORD QUARTER`. Hold on the trophy beside a completely unusable system.
```


---

# Animation 06 — `kill` — send a signal

## Chapter 7 — Processes

## Source animation sequence

A tiny post office marked `kill` sorts envelopes.

One says:

`TERM — Request to terminate.`

Another says:

`HUP — Hangup.`

Another says:

`STOP — Don't move.`

At the far end sits a black envelope stamped:

`KILL — CANNOT BE REFUSED`

The clerk reaches for it.

Every other envelope in the room becomes extremely cooperative.

## Sequence lock

**Background:** midnight blue `#111D38`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

CLERK_A: short postal clerk pictogram with cylindrical cap and rectangular body; ENVELOPE_TERM/HUP/STOP/KILL: identical envelope geometry, distinguished only by label and KILL being visually darker. Clerk must never change.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
CLERK_A: short postal clerk pictogram with cylindrical cap and rectangular body; ENVELOPE_TERM/HUP/STOP/KILL: identical envelope geometry, distinguished only by label and KILL being visually darker. Clerk must never change.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `06_kill_frame_00.png`

**Used as:** Cut 01 START

A miniature post office labeled `kill`. A clerk sorts envelopes into slots.

### Frame 01 — `06_kill_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

The clerk noticing a distant black envelope.

### Frame 02 — `06_kill_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

One hand just about to touch it.

### Frame 03 — `06_kill_frame_03.png`

**Used as:** Cut 03 END

The clerk looking mildly pleased with improved workflow.

## Cut 01 — 00:00–05:00

**START:** `06_kill_frame_00.png`

**END:** `06_kill_frame_01.png`

A miniature post office labeled `kill`. A clerk sorts envelopes into slots. Show three envelopes in succession, each cleanly readable: `TERM — REQUEST TO TERMINATE`, `HUP — HANGUP`, `STOP — DON'T MOVE`. Each slides to a different chute. End with the clerk noticing a distant black envelope.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight blue #111D38
Character/prop lock: CLERK_A: short postal clerk pictogram with cylindrical cap and rectangular body; ENVELOPE_TERM/HUP/STOP/KILL: identical envelope geometry, distinguished only by label and KILL being visually darker. Clerk must never change.
Reference board: use the canonical sequence reference board for Animation 06.
START FRAME: 06_kill_frame_00.png
END FRAME: 06_kill_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A miniature post office labeled `kill`. A clerk sorts envelopes into slots. Show three envelopes in succession, each cleanly readable: `TERM — REQUEST TO TERMINATE`, `HUP — HANGUP`, `STOP — DON'T MOVE`. Each slides to a different chute. End with the clerk noticing a distant black envelope.
```

## Cut 02 — 05:00–10:00

**START:** `06_kill_frame_01.png`

**END:** `06_kill_frame_02.png`

Camera pans a little along the sorting bench to reveal the black envelope alone under a warning lamp: `KILL — CANNOT BE REFUSED`. The clerk approaches it with exaggerated procedural caution, using tongs or a tiny cart, never emoting. End with one hand just about to touch it.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight blue #111D38
Character/prop lock: CLERK_A: short postal clerk pictogram with cylindrical cap and rectangular body; ENVELOPE_TERM/HUP/STOP/KILL: identical envelope geometry, distinguished only by label and KILL being visually darker. Clerk must never change.
Reference board: use the canonical sequence reference board for Animation 06.
START FRAME: 06_kill_frame_01.png
END FRAME: 06_kill_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Camera pans a little along the sorting bench to reveal the black envelope alone under a warning lamp: `KILL — CANNOT BE REFUSED`. The clerk approaches it with exaggerated procedural caution, using tongs or a tiny cart, never emoting. End with one hand just about to touch it.
```

## Cut 03 — 10:00–15:00

**START:** `06_kill_frame_02.png`

**END:** `06_kill_frame_03.png`

The instant the clerk touches the black envelope, every other envelope, chute, parcel and waiting process snaps into perfect obedient alignment. No explosion. No violence. Just terrifying administrative cooperation. Hold on the clerk looking mildly pleased with improved workflow.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight blue #111D38
Character/prop lock: CLERK_A: short postal clerk pictogram with cylindrical cap and rectangular body; ENVELOPE_TERM/HUP/STOP/KILL: identical envelope geometry, distinguished only by label and KILL being visually darker. Clerk must never change.
Reference board: use the canonical sequence reference board for Animation 06.
START FRAME: 06_kill_frame_02.png
END FRAME: 06_kill_frame_03.png

CUT 03 OF 03 — 10:00–15:00
The instant the clerk touches the black envelope, every other envelope, chute, parcel and waiting process snaps into perfect obedient alignment. No explosion. No violence. Just terrifying administrative cooperation. Hold on the clerk looking mildly pleased with improved workflow.
```


---

# Animation 07 — `df` — filesystem free space

## Chapter 8 — Storage

## Source animation sequence

`df`

A disk appears divided into **USED** and **FREE**.

The FREE section notices the label.

A tiny demonstration begins.

`FREE SPACE!`

`FREE SPACE!`

A system administrator changes the sign to:

`AVAILABLE`

The demonstrators stare at it.

One slowly lowers its placard.

Another asks:

**"Available for what?"**

The administrator allocates the block.

The demonstration ends for technical reasons.

## Sequence lock

**Background:** warm cream `#C8BE9C`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

ADMIN_A: tall narrow administrator with square head and one pointer arm; PROTESTER_BASE: tiny block-person cloned for all demonstrators. Placards are identical rectangles. Preserve exact clones across cuts.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
ADMIN_A: tall narrow administrator with square head and one pointer arm; PROTESTER_BASE: tiny block-person cloned for all demonstrators. Placards are identical rectangles. Preserve exact clones across cuts.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `07_df_frame_00.png`

**Used as:** Cut 01 START

A disk is shown as a simple circular or rectangular storage diagram split into `USED` and `FREE`.

### Frame 01 — `07_df_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

An administrator enters.

### Frame 02 — `07_df_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

A caption card from one protester: `AVAILABLE FOR WHAT?`.

### Frame 03 — `07_df_frame_03.png`

**Used as:** Cut 03 END

The newly smaller AVAILABLE region.

## Cut 01 — 00:00–05:00

**START:** `07_df_frame_00.png`

**END:** `07_df_frame_01.png`

A disk is shown as a simple circular or rectangular storage diagram split into `USED` and `FREE`. The FREE region looks at its own label, then tiny demonstrators materialize inside it carrying `FREE SPACE!` placards. Their march is neat, repetitive and bureaucratic. End as an administrator enters.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: warm cream #C8BE9C
Character/prop lock: ADMIN_A: tall narrow administrator with square head and one pointer arm; PROTESTER_BASE: tiny block-person cloned for all demonstrators. Placards are identical rectangles. Preserve exact clones across cuts.
Reference board: use the canonical sequence reference board for Animation 07.
START FRAME: 07_df_frame_00.png
END FRAME: 07_df_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A disk is shown as a simple circular or rectangular storage diagram split into `USED` and `FREE`. The FREE region looks at its own label, then tiny demonstrators materialize inside it carrying `FREE SPACE!` placards. Their march is neat, repetitive and bureaucratic. End as an administrator enters.
```

## Cut 02 — 05:00–10:00

**START:** `07_df_frame_01.png`

**END:** `07_df_frame_02.png`

The administrator calmly removes the word `FREE` and replaces it with `AVAILABLE`. The march stops. One demonstrator slowly lowers a placard; another turns its sign over as if checking the wording. End with a caption card from one protester: `AVAILABLE FOR WHAT?`

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: warm cream #C8BE9C
Character/prop lock: ADMIN_A: tall narrow administrator with square head and one pointer arm; PROTESTER_BASE: tiny block-person cloned for all demonstrators. Placards are identical rectangles. Preserve exact clones across cuts.
Reference board: use the canonical sequence reference board for Animation 07.
START FRAME: 07_df_frame_01.png
END FRAME: 07_df_frame_02.png

CUT 02 OF 03 — 05:00–10:00
The administrator calmly removes the word `FREE` and replaces it with `AVAILABLE`. The march stops. One demonstrator slowly lowers a placard; another turns its sign over as if checking the wording. End with a caption card from one protester: `AVAILABLE FOR WHAT?`
```

## Cut 03 — 10:00–15:00

**START:** `07_df_frame_02.png`

**END:** `07_df_frame_03.png`

The administrator points to one block of the `AVAILABLE` region; the block is instantly allocated and recolored `USED`, taking the protester standing on it with it. The remaining demonstration disperses for purely technical reasons. Hold on the newly smaller AVAILABLE region.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: warm cream #C8BE9C
Character/prop lock: ADMIN_A: tall narrow administrator with square head and one pointer arm; PROTESTER_BASE: tiny block-person cloned for all demonstrators. Placards are identical rectangles. Preserve exact clones across cuts.
Reference board: use the canonical sequence reference board for Animation 07.
START FRAME: 07_df_frame_02.png
END FRAME: 07_df_frame_03.png

CUT 03 OF 03 — 10:00–15:00
The administrator points to one block of the `AVAILABLE` region; the block is instantly allocated and recolored `USED`, taking the protester standing on it with it. The remaining demonstration disperses for purely technical reasons. Hold on the newly smaller AVAILABLE region.
```


---

# Animation 08 — `rm` — remove

## Chapter 8 — Storage

## Source animation sequence

A file sits beneath a sign bearing its filename.

`rm filename`

A hand removes the sign.

The file is still sitting there.

A philosopher runs in, points triumphantly, and begins writing a book.

A small process is holding the file open with one hand.

The process finishes, lets go, and walks away.

The file drops instantly through the floor.

The philosopher looks down into the hole.

A new file rises into the same space.

It is labelled:

`draft_final.txt`

The philosopher closes the book.

## Sequence lock

**Background:** dusty pink `#B56B78`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

FILE_A: cream rectangular file-card character with one folded corner; PROCESS_A: tiny dark holder character with hook-like hand; PHILOSOPHER_A: tall thin figure with triangular nose and book. Keep all three exact across frames.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
FILE_A: cream rectangular file-card character with one folded corner; PROCESS_A: tiny dark holder character with hook-like hand; PHILOSOPHER_A: tall thin figure with triangular nose and book. Keep all three exact across frames.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `08_rm_frame_00.png`

**Used as:** Cut 01 START

A file is represented as a simple solid object sitting under a freestanding filename sign.

### Frame 01 — `08_rm_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Triumphant philosopher.

### Frame 02 — `08_rm_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

End when the process finishes its task and looks at the file.

### Frame 03 — `08_rm_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

Empty hole.

### Frame 04 — `08_rm_frame_04.png`

**Used as:** Cut 04 END

The replacement occupying the reused space.

## Cut 01 — 00:00–05:00

**START:** `08_rm_frame_00.png`

**END:** `08_rm_frame_01.png`

A file is represented as a simple solid object sitting under a freestanding filename sign. `rm filename` appears. A hand removes only the sign; the file object remains sitting there. A philosopher rushes in, points at the still-present file, and opens a huge notebook. End on triumphant philosopher.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty pink #B56B78
Character/prop lock: FILE_A: cream rectangular file-card character with one folded corner; PROCESS_A: tiny dark holder character with hook-like hand; PHILOSOPHER_A: tall thin figure with triangular nose and book. Keep all three exact across frames.
Reference board: use the canonical sequence reference board for Animation 08.
START FRAME: 08_rm_frame_00.png
END FRAME: 08_rm_frame_01.png

CUT 01 OF 04 — 00:00–05:00
A file is represented as a simple solid object sitting under a freestanding filename sign. `rm filename` appears. A hand removes only the sign; the file object remains sitting there. A philosopher rushes in, points at the still-present file, and opens a huge notebook. End on triumphant philosopher.
```

## Cut 02 — 05:00–10:00

**START:** `08_rm_frame_01.png`

**END:** `08_rm_frame_02.png`

Continue. Reveal a tiny process at frame edge physically holding the file with one hand. The philosopher writes furiously while the process continues working. The removed filename sign lies on the floor. End when the process finishes its task and looks at the file.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty pink #B56B78
Character/prop lock: FILE_A: cream rectangular file-card character with one folded corner; PROCESS_A: tiny dark holder character with hook-like hand; PHILOSOPHER_A: tall thin figure with triangular nose and book. Keep all three exact across frames.
Reference board: use the canonical sequence reference board for Animation 08.
START FRAME: 08_rm_frame_01.png
END FRAME: 08_rm_frame_02.png

CUT 02 OF 04 — 05:00–10:00
Continue. Reveal a tiny process at frame edge physically holding the file with one hand. The philosopher writes furiously while the process continues working. The removed filename sign lies on the floor. End when the process finishes its task and looks at the file.
```

## Cut 03 — 10:00–15:00

**START:** `08_rm_frame_02.png`

**END:** `08_rm_frame_03.png`

The process lets go and walks away. The instant its hand releases, the file drops vertically through a clean trapdoor in the floor. The philosopher freezes mid-sentence, then peers into the hole. End on empty hole.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty pink #B56B78
Character/prop lock: FILE_A: cream rectangular file-card character with one folded corner; PROCESS_A: tiny dark holder character with hook-like hand; PHILOSOPHER_A: tall thin figure with triangular nose and book. Keep all three exact across frames.
Reference board: use the canonical sequence reference board for Animation 08.
START FRAME: 08_rm_frame_02.png
END FRAME: 08_rm_frame_03.png

CUT 03 OF 04 — 10:00–15:00
The process lets go and walks away. The instant its hand releases, the file drops vertically through a clean trapdoor in the floor. The philosopher freezes mid-sentence, then peers into the hole. End on empty hole.
```

## Cut 04 — 15:00–20:00

**START:** `08_rm_frame_03.png`

**END:** `08_rm_frame_04.png`

A new file rises into the same storage slot from below, neatly labeled `draft_final.txt`. The philosopher compares the new file, the hole, and the unfinished book, then slowly closes the book. No commentary. Hold on the replacement occupying the reused space.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty pink #B56B78
Character/prop lock: FILE_A: cream rectangular file-card character with one folded corner; PROCESS_A: tiny dark holder character with hook-like hand; PHILOSOPHER_A: tall thin figure with triangular nose and book. Keep all three exact across frames.
Reference board: use the canonical sequence reference board for Animation 08.
START FRAME: 08_rm_frame_03.png
END FRAME: 08_rm_frame_04.png

CUT 04 OF 04 — 15:00–20:00
A new file rises into the same storage slot from below, neatly labeled `draft_final.txt`. The philosopher compares the new file, the hole, and the unfinished book, then slowly closes the book. No commentary. Hold on the replacement occupying the reused space.
```


---

# Animation 09 — `source` — source

## Chapter 9 — Shell Scripting

## Source animation sequence

A shell sits at a desk.

A script waits outside with a visitor badge.

The shell runs the script normally.

The script works in a tiny rented office, rearranges all the furniture, then leaves.

The shell's office remains untouched.

Reset.

The shell types:

`source script.sh`

The script walks directly into the shell's office.

It moves the desk.

Renames three drawers.

Changes a road sign marked `PATH`.

Replaces the coffee with an environment variable.

Then leaves.

The shell looks around.

A note on the desk reads:

**Changes saved.**

## Sequence lock

**Background:** sage green `#708A72`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

SHELL_A: seated rectangular shell-office worker with round head; SCRIPT_A: thin walking sheet-of-paper character with folded corner; DESK_A and PATH_SIGN_A retain identical geometry before/after rearrangement.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
SHELL_A: seated rectangular shell-office worker with round head; SCRIPT_A: thin walking sheet-of-paper character with folded corner; DESK_A and PATH_SIGN_A retain identical geometry before/after rearrangement.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `09_source_frame_00.png`

**Used as:** Cut 01 START

A shell character sits at an office desk. A script waits outside the office with a visitor badge.

### Frame 01 — `09_source_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Script leaving the side-office.

### Frame 02 — `09_source_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Script's hand on the shell's desk.

### Frame 03 — `09_source_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

Script exiting through the door.

### Frame 04 — `09_source_frame_04.png`

**Used as:** Cut 04 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `09_source_frame_00.png`

**END:** `09_source_frame_01.png`

A shell character sits at an office desk. A script waits outside the office with a visitor badge. The shell runs the script normally: a tiny rented side-office appears; the script enters it and rearranges its furniture. The shell's main office remains untouched. End with script leaving the side-office.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sage green #708A72
Character/prop lock: SHELL_A: seated rectangular shell-office worker with round head; SCRIPT_A: thin walking sheet-of-paper character with folded corner; DESK_A and PATH_SIGN_A retain identical geometry before/after rearrangement.
Reference board: use the canonical sequence reference board for Animation 09.
START FRAME: 09_source_frame_00.png
END FRAME: 09_source_frame_01.png

CUT 01 OF 04 — 00:00–05:00
A shell character sits at an office desk. A script waits outside the office with a visitor badge. The shell runs the script normally: a tiny rented side-office appears; the script enters it and rearranges its furniture. The shell's main office remains untouched. End with script leaving the side-office.
```

## Cut 02 — 05:00–10:00

**START:** `09_source_frame_01.png`

**END:** `09_source_frame_02.png`

A visual `RESET` card flips the set back. The shell types `source script.sh`. This time the visitor badge is discarded and the script walks directly into the shell's own office. End with script's hand on the shell's desk.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sage green #708A72
Character/prop lock: SHELL_A: seated rectangular shell-office worker with round head; SCRIPT_A: thin walking sheet-of-paper character with folded corner; DESK_A and PATH_SIGN_A retain identical geometry before/after rearrangement.
Reference board: use the canonical sequence reference board for Animation 09.
START FRAME: 09_source_frame_01.png
END FRAME: 09_source_frame_02.png

CUT 02 OF 04 — 05:00–10:00
A visual `RESET` card flips the set back. The shell types `source script.sh`. This time the visitor badge is discarded and the script walks directly into the shell's own office. End with script's hand on the shell's desk.
```

## Cut 03 — 10:00–15:00

**START:** `09_source_frame_02.png`

**END:** `09_source_frame_03.png`

The script briskly moves the desk, renames three filing drawers, swivels a road sign marked `PATH`, and replaces the coffee cup with a small box labeled `ENV`. All changes are literal, clean, and diagrammatic. End with script exiting through the door.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sage green #708A72
Character/prop lock: SHELL_A: seated rectangular shell-office worker with round head; SCRIPT_A: thin walking sheet-of-paper character with folded corner; DESK_A and PATH_SIGN_A retain identical geometry before/after rearrangement.
Reference board: use the canonical sequence reference board for Animation 09.
START FRAME: 09_source_frame_02.png
END FRAME: 09_source_frame_03.png

CUT 03 OF 04 — 10:00–15:00
The script briskly moves the desk, renames three filing drawers, swivels a road sign marked `PATH`, and replaces the coffee cup with a small box labeled `ENV`. All changes are literal, clean, and diagrammatic. End with script exiting through the door.
```

## Cut 04 — 15:00–20:00

**START:** `09_source_frame_03.png`

**END:** `09_source_frame_04.png`

The shell looks around at the rearranged office. A single note on the moved desk unfolds: `CHANGES SAVED.` The shell looks toward the door the script used, then back at the altered `PATH` sign. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sage green #708A72
Character/prop lock: SHELL_A: seated rectangular shell-office worker with round head; SCRIPT_A: thin walking sheet-of-paper character with folded corner; DESK_A and PATH_SIGN_A retain identical geometry before/after rearrangement.
Reference board: use the canonical sequence reference board for Animation 09.
START FRAME: 09_source_frame_03.png
END FRAME: 09_source_frame_04.png

CUT 04 OF 04 — 15:00–20:00
The shell looks around at the rearranged office. A single note on the moved desk unfolds: `CHANGES SAVED.` The shell looks toward the door the script used, then back at the altered `PATH` sign. Hold.
```


---

# Animation 10 — `ping` — ping

## Chapter 10 — Networking

## Source animation sequence

A tiny packet crosses a network and knocks on a distant machine.

**PING?**

The machine changes its badge from **REQUEST** to **REPLY** and sends it back.

The reply runs home.

A stopwatch stops.

Again.

Again.

On the fourth trip the packet knocks.

Nothing.

A polite notice appears:

**No reply.**

Under it:

**The host may be unavailable.**

A smaller line appears:

**Or avoiding you.**

An even smaller line:

**We really hope it's the first one.**

Beside the notice appears a calming picture of a router.

It is not especially calming.

## Sequence lock

**Background:** dark navy `#07182B`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

PACKET_A: tiny rounded square courier with two stick legs; HOST_A: simple rectangular machine-face with one slot; ROUTER_PICTURE_A: fixed geometric router icon. Packet and host silhouettes must not mutate.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
PACKET_A: tiny rounded square courier with two stick legs; HOST_A: simple rectangular machine-face with one slot; ROUTER_PICTURE_A: fixed geometric router icon. Packet and host silhouettes must not mutate.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `10_ping_frame_00.png`

**Used as:** Cut 01 START

A tiny packet leaves a local machine and traverses a diagrammatic network of nodes. It knocks on a distant machine with a small `PING?` tag.

### Frame 01 — `10_ping_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

First successful round trip.

### Frame 02 — `10_ping_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

A beat of silence beside the closed machine.

### Frame 03 — `10_ping_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

Empty space beside the notice.

### Frame 04 — `10_ping_frame_04.png`

**Used as:** Cut 04 END

The mismatch between reassurance and router.

## Cut 01 — 00:00–05:00

**START:** `10_ping_frame_00.png`

**END:** `10_ping_frame_01.png`

A tiny packet leaves a local machine and traverses a diagrammatic network of nodes. It knocks on a distant machine with a small `PING?` tag. The machine flips the packet's badge from `REQUEST` to `REPLY` and sends it back. A stopwatch at home stops. End on first successful round trip.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #07182B
Character/prop lock: PACKET_A: tiny rounded square courier with two stick legs; HOST_A: simple rectangular machine-face with one slot; ROUTER_PICTURE_A: fixed geometric router icon. Packet and host silhouettes must not mutate.
Reference board: use the canonical sequence reference board for Animation 10.
START FRAME: 10_ping_frame_00.png
END FRAME: 10_ping_frame_01.png

CUT 01 OF 04 — 00:00–05:00
A tiny packet leaves a local machine and traverses a diagrammatic network of nodes. It knocks on a distant machine with a small `PING?` tag. The machine flips the packet's badge from `REQUEST` to `REPLY` and sends it back. A stopwatch at home stops. End on first successful round trip.
```

## Cut 02 — 05:00–10:00

**START:** `10_ping_frame_01.png`

**END:** `10_ping_frame_02.png`

Repeat two more round trips faster, using the same path and stopwatch. Keep the action almost mechanical. On the fourth trip, the packet reaches the distant machine and knocks. Nothing happens. The packet waits. End on a beat of silence beside the closed machine.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #07182B
Character/prop lock: PACKET_A: tiny rounded square courier with two stick legs; HOST_A: simple rectangular machine-face with one slot; ROUTER_PICTURE_A: fixed geometric router icon. Packet and host silhouettes must not mutate.
Reference board: use the canonical sequence reference board for Animation 10.
START FRAME: 10_ping_frame_01.png
END FRAME: 10_ping_frame_02.png

CUT 02 OF 04 — 05:00–10:00
Repeat two more round trips faster, using the same path and stopwatch. Keep the action almost mechanical. On the fourth trip, the packet reaches the distant machine and knocks. Nothing happens. The packet waits. End on a beat of silence beside the closed machine.
```

## Cut 03 — 10:00–15:00

**START:** `10_ping_frame_02.png`

**END:** `10_ping_frame_03.png`

A polite notice panel slides in: `NO REPLY.` Then beneath it, smaller: `THE HOST MAY BE UNAVAILABLE.` Then smaller still: `OR AVOIDING YOU.` Let each line arrive as a precise information layer, not a joke reaction. End with empty space beside the notice.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #07182B
Character/prop lock: PACKET_A: tiny rounded square courier with two stick legs; HOST_A: simple rectangular machine-face with one slot; ROUTER_PICTURE_A: fixed geometric router icon. Packet and host silhouettes must not mutate.
Reference board: use the canonical sequence reference board for Animation 10.
START FRAME: 10_ping_frame_02.png
END FRAME: 10_ping_frame_03.png

CUT 03 OF 04 — 10:00–15:00
A polite notice panel slides in: `NO REPLY.` Then beneath it, smaller: `THE HOST MAY BE UNAVAILABLE.` Then smaller still: `OR AVOIDING YOU.` Let each line arrive as a precise information layer, not a joke reaction. End with empty space beside the notice.
```

## Cut 04 — 15:00–20:00

**START:** `10_ping_frame_03.png`

**END:** `10_ping_frame_04.png`

An even smaller line adds `WE REALLY HOPE IT'S THE FIRST ONE.` Beside the notice, a calming illustration of a router appears inside a pastel circle. The router is visibly severe and uncalming. Hold on the mismatch between reassurance and router.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #07182B
Character/prop lock: PACKET_A: tiny rounded square courier with two stick legs; HOST_A: simple rectangular machine-face with one slot; ROUTER_PICTURE_A: fixed geometric router icon. Packet and host silhouettes must not mutate.
Reference board: use the canonical sequence reference board for Animation 10.
START FRAME: 10_ping_frame_03.png
END FRAME: 10_ping_frame_04.png

CUT 04 OF 04 — 15:00–20:00
An even smaller line adds `WE REALLY HOPE IT'S THE FIRST ONE.` Beside the notice, a calming illustration of a router appears inside a pastel circle. The router is visibly severe and uncalming. Hold on the mismatch between reassurance and router.
```


---

# Animation 11 — `ssh` — secure shell

## Chapter 10 — Networking

## Source animation sequence

Two computers sit on opposite planets.

One is labelled:

`HERE`

The other:

`THERE`

The user runs `ssh`.

A shell from THERE slides through a locked tunnel and appears on the screen HERE.

The user types a command.

The letters travel down the tunnel.

Something changes on THERE.

The labels begin to wobble.

`HERE` changes to:

`HERE, PHYSICALLY`

`THERE` changes to:

`HERE, AS FAR AS THE SHELL IS CONCERNED`

The user runs:

`pwd`

Both planets point at themselves.

## Sequence lock

**Background:** midnight blue `#13224A`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

USER_A: angular human pictogram at HERE; COMPUTER_HERE and COMPUTER_THERE: identical squat terminal boxes; REMOTE_SHELL_A: one shell-shaped flat panel. Planet icons and characters retain exact scale ratios.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
USER_A: angular human pictogram at HERE; COMPUTER_HERE and COMPUTER_THERE: identical squat terminal boxes; REMOTE_SHELL_A: one shell-shaped flat panel. Planet icons and characters retain exact scale ratios.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `11_ssh_frame_00.png`

**Used as:** Cut 01 START

Two flat planets sit far apart: left computer labeled `HERE`, right computer labeled `THERE`.

### Frame 01 — `11_ssh_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Remote shell visibly local.

### Frame 02 — `11_ssh_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

The changed object at THERE while the command text still originates HERE.

### Frame 03 — `11_ssh_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

The contradictory double-HERE diagram.

### Frame 04 — `11_ssh_frame_04.png`

**Used as:** Cut 04 END

Both planets confidently asserting location.

## Cut 01 — 00:00–05:00

**START:** `11_ssh_frame_00.png`

**END:** `11_ssh_frame_01.png`

Two flat planets sit far apart: left computer labeled `HERE`, right computer labeled `THERE`. A user at HERE invokes `ssh`. A dark locked tunnel draws itself between planets. A shell-shaped panel from THERE slides through the tunnel and settles on HERE's screen. End with remote shell visibly local.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight blue #13224A
Character/prop lock: USER_A: angular human pictogram at HERE; COMPUTER_HERE and COMPUTER_THERE: identical squat terminal boxes; REMOTE_SHELL_A: one shell-shaped flat panel. Planet icons and characters retain exact scale ratios.
Reference board: use the canonical sequence reference board for Animation 11.
START FRAME: 11_ssh_frame_00.png
END FRAME: 11_ssh_frame_01.png

CUT 01 OF 04 — 00:00–05:00
Two flat planets sit far apart: left computer labeled `HERE`, right computer labeled `THERE`. A user at HERE invokes `ssh`. A dark locked tunnel draws itself between planets. A shell-shaped panel from THERE slides through the tunnel and settles on HERE's screen. End with remote shell visibly local.
```

## Cut 02 — 05:00–10:00

**START:** `11_ssh_frame_01.png`

**END:** `11_ssh_frame_02.png`

The user types a command. The letters themselves travel down the tunnel to THERE, where a simple object changes state. The user's hands and keyboard remain at HERE. End on the changed object at THERE while the command text still originates HERE.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight blue #13224A
Character/prop lock: USER_A: angular human pictogram at HERE; COMPUTER_HERE and COMPUTER_THERE: identical squat terminal boxes; REMOTE_SHELL_A: one shell-shaped flat panel. Planet icons and characters retain exact scale ratios.
Reference board: use the canonical sequence reference board for Animation 11.
START FRAME: 11_ssh_frame_01.png
END FRAME: 11_ssh_frame_02.png

CUT 02 OF 04 — 05:00–10:00
The user types a command. The letters themselves travel down the tunnel to THERE, where a simple object changes state. The user's hands and keyboard remain at HERE. End on the changed object at THERE while the command text still originates HERE.
```

## Cut 03 — 10:00–15:00

**START:** `11_ssh_frame_02.png`

**END:** `11_ssh_frame_03.png`

The labels `HERE` and `THERE` wobble, then mechanically relabel: left becomes `HERE, PHYSICALLY`; right becomes `HERE, AS FAR AS THE SHELL IS CONCERNED`. Keep both planets in frame and the tunnel connected. End on the contradictory double-HERE diagram.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight blue #13224A
Character/prop lock: USER_A: angular human pictogram at HERE; COMPUTER_HERE and COMPUTER_THERE: identical squat terminal boxes; REMOTE_SHELL_A: one shell-shaped flat panel. Planet icons and characters retain exact scale ratios.
Reference board: use the canonical sequence reference board for Animation 11.
START FRAME: 11_ssh_frame_02.png
END FRAME: 11_ssh_frame_03.png

CUT 03 OF 04 — 10:00–15:00
The labels `HERE` and `THERE` wobble, then mechanically relabel: left becomes `HERE, PHYSICALLY`; right becomes `HERE, AS FAR AS THE SHELL IS CONCERNED`. Keep both planets in frame and the tunnel connected. End on the contradictory double-HERE diagram.
```

## Cut 04 — 15:00–20:00

**START:** `11_ssh_frame_03.png`

**END:** `11_ssh_frame_04.png`

The user types `pwd`. Each planet sprouts a large pointing arrow toward itself at exactly the same moment. The arrows cross through the tunnel. Hold on both planets confidently asserting location.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: midnight blue #13224A
Character/prop lock: USER_A: angular human pictogram at HERE; COMPUTER_HERE and COMPUTER_THERE: identical squat terminal boxes; REMOTE_SHELL_A: one shell-shaped flat panel. Planet icons and characters retain exact scale ratios.
Reference board: use the canonical sequence reference board for Animation 11.
START FRAME: 11_ssh_frame_03.png
END FRAME: 11_ssh_frame_04.png

CUT 04 OF 04 — 15:00–20:00
The user types `pwd`. Each planet sprouts a large pointing arrow toward itself at exactly the same moment. The arrows cross through the tunnel. Hold on both planets confidently asserting location.
```


---

# Animation 12 — `harden-check` — harden check

## Chapter 11 — Self-Knowledge

## Source animation sequence

A cheerful little computer made of modelling clay sits on a desk.

`harden-check`

A pointer identifies a soft spot.

The user fixes it.

Another pointer appears.

The user fixes that too.

At last:

`No obvious issues found.`

A clerk from Documentation enters, removes the word **obvious**, and carries it away for review.

The message now reads:

`No issues found.`

Security immediately puts **obvious** back.

## Sequence lock

**Background:** clay pink `#AE7775`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

COMPUTER_A: soft rounded modelling-clay-like computer pictogram built from simple flat blobs; DOC_CLERK_A: severe narrow figure with folder; SECURITY_A: two identical blocky security figures. Preserve COMPUTER_A's exact dents/patch geometry from frame to frame.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
COMPUTER_A: soft rounded modelling-clay-like computer pictogram built from simple flat blobs; DOC_CLERK_A: severe narrow figure with folder; SECURITY_A: two identical blocky security figures. Preserve COMPUTER_A's exact dents/patch geometry from frame to frame.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `12_harden_check_frame_00.png`

**Used as:** Cut 01 START

A cheerful little computer made from soft modelling-clay shapes sits on a desk. `harden-check` appears as a label.

### Frame 01 — `12_harden_check_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

The now tidy computer.

### Frame 02 — `12_harden_check_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

The remaining card now reads `NO ISSUES FOUND.` End on the dangerously absolute claim.

### Frame 03 — `12_harden_check_frame_03.png`

**Used as:** Cut 03 END

`NO OBVIOUS ISSUES FOUND.` as if a constitutional crisis has been narrowly avoided.

## Cut 01 — 00:00–05:00

**START:** `12_harden_check_frame_00.png`

**END:** `12_harden_check_frame_01.png`

A cheerful little computer made from soft modelling-clay shapes sits on a desk. `harden-check` appears as a label. A pointer identifies one soft spot; the user repairs it with a small rigid patch. A second pointer immediately identifies another soft spot; user patches that too. End on the now tidy computer.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: clay pink #AE7775
Character/prop lock: COMPUTER_A: soft rounded modelling-clay-like computer pictogram built from simple flat blobs; DOC_CLERK_A: severe narrow figure with folder; SECURITY_A: two identical blocky security figures. Preserve COMPUTER_A's exact dents/patch geometry from frame to frame.
Reference board: use the canonical sequence reference board for Animation 12.
START FRAME: 12_harden_check_frame_00.png
END FRAME: 12_harden_check_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A cheerful little computer made from soft modelling-clay shapes sits on a desk. `harden-check` appears as a label. A pointer identifies one soft spot; the user repairs it with a small rigid patch. A second pointer immediately identifies another soft spot; user patches that too. End on the now tidy computer.
```

## Cut 02 — 05:00–10:00

**START:** `12_harden_check_frame_01.png`

**END:** `12_harden_check_frame_02.png`

A message card appears: `NO OBVIOUS ISSUES FOUND.` A severe Documentation clerk enters, carefully peels the word `OBVIOUS` out of the sentence and walks away carrying it in a folder. The remaining card now reads `NO ISSUES FOUND.` End on the dangerously absolute claim.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: clay pink #AE7775
Character/prop lock: COMPUTER_A: soft rounded modelling-clay-like computer pictogram built from simple flat blobs; DOC_CLERK_A: severe narrow figure with folder; SECURITY_A: two identical blocky security figures. Preserve COMPUTER_A's exact dents/patch geometry from frame to frame.
Reference board: use the canonical sequence reference board for Animation 12.
START FRAME: 12_harden_check_frame_01.png
END FRAME: 12_harden_check_frame_02.png

CUT 02 OF 03 — 05:00–10:00
A message card appears: `NO OBVIOUS ISSUES FOUND.` A severe Documentation clerk enters, carefully peels the word `OBVIOUS` out of the sentence and walks away carrying it in a folder. The remaining card now reads `NO ISSUES FOUND.` End on the dangerously absolute claim.
```

## Cut 03 — 10:00–15:00

**START:** `12_harden_check_frame_02.png`

**END:** `12_harden_check_frame_03.png`

Security personnel rush in, retrieve `OBVIOUS` from Documentation, and snap it back into the message in exactly its original position. Everyone returns to neutral poses. Hold on `NO OBVIOUS ISSUES FOUND.` as if a constitutional crisis has been narrowly avoided.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: clay pink #AE7775
Character/prop lock: COMPUTER_A: soft rounded modelling-clay-like computer pictogram built from simple flat blobs; DOC_CLERK_A: severe narrow figure with folder; SECURITY_A: two identical blocky security figures. Preserve COMPUTER_A's exact dents/patch geometry from frame to frame.
Reference board: use the canonical sequence reference board for Animation 12.
START FRAME: 12_harden_check_frame_02.png
END FRAME: 12_harden_check_frame_03.png

CUT 03 OF 03 — 10:00–15:00
Security personnel rush in, retrieve `OBVIOUS` from Documentation, and snap it back into the message in exactly its original position. Everyone returns to neutral poses. Hold on `NO OBVIOUS ISSUES FOUND.` as if a constitutional crisis has been narrowly avoided.
```


---

# Animation 13 — `git commit` — git commit

## Chapter 12 — Version Control

## Source animation sequence

A user makes a small change.

`git commit`

A stone monument erupts from the floor:

**FIX**

Another change.

Another monument:

**FIX AGAIN**

Another:

**FINAL FIX**

The room fills with monuments until the user can no longer reach the computer.

A future archaeologist squeezes between them, brushes dust from one inscription and whispers:

**"Remarkable. They knew."**

## Sequence lock

**Background:** sandstone `#A98D67`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

USER_A: simple angular human pictogram; MONUMENT_BASE: identical stone slab shape cloned for every commit with label changed only; ARCHAEOLOGIST_A: thin bent figure with brush. No monument shape drift.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
USER_A: simple angular human pictogram; MONUMENT_BASE: identical stone slab shape cloned for every commit with label changed only; ARCHAEOLOGIST_A: thin bent figure with brush. No monument shape drift.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `13_git_commit_frame_00.png`

**Used as:** Cut 01 START

A user makes one tiny edit at a terminal, then `git commit`. A heavy stone monument rises from the floor beside the desk marked `FIX`.

### Frame 01 — `13_git_commit_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

The floor begins to bulge again.

### Frame 02 — `13_git_commit_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

User trapped behind history.

### Frame 03 — `13_git_commit_frame_03.png`

**Used as:** Cut 03 END

THEY KNEW.` Hold on the archaeologist admiring what was plainly not final.

## Cut 01 — 00:00–05:00

**START:** `13_git_commit_frame_00.png`

**END:** `13_git_commit_frame_01.png`

A user makes one tiny edit at a terminal, then `git commit`. A heavy stone monument rises from the floor beside the desk marked `FIX`. The user makes a second tiny edit. End as the floor begins to bulge again.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sandstone #A98D67
Character/prop lock: USER_A: simple angular human pictogram; MONUMENT_BASE: identical stone slab shape cloned for every commit with label changed only; ARCHAEOLOGIST_A: thin bent figure with brush. No monument shape drift.
Reference board: use the canonical sequence reference board for Animation 13.
START FRAME: 13_git_commit_frame_00.png
END FRAME: 13_git_commit_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A user makes one tiny edit at a terminal, then `git commit`. A heavy stone monument rises from the floor beside the desk marked `FIX`. The user makes a second tiny edit. End as the floor begins to bulge again.
```

## Cut 02 — 05:00–10:00

**START:** `13_git_commit_frame_01.png`

**END:** `13_git_commit_frame_02.png`

Second monument erupts: `FIX AGAIN`. Third edit; third monument: `FINAL FIX`. More monuments rapidly appear with increasingly familiar short inscriptions until they crowd the room and block the user's route back to the computer. End with user trapped behind history.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sandstone #A98D67
Character/prop lock: USER_A: simple angular human pictogram; MONUMENT_BASE: identical stone slab shape cloned for every commit with label changed only; ARCHAEOLOGIST_A: thin bent figure with brush. No monument shape drift.
Reference board: use the canonical sequence reference board for Animation 13.
START FRAME: 13_git_commit_frame_01.png
END FRAME: 13_git_commit_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Second monument erupts: `FIX AGAIN`. Third edit; third monument: `FINAL FIX`. More monuments rapidly appear with increasingly familiar short inscriptions until they crowd the room and block the user's route back to the computer. End with user trapped behind history.
```

## Cut 03 — 10:00–15:00

**START:** `13_git_commit_frame_02.png`

**END:** `13_git_commit_frame_03.png`

Time jumps forward through a simple color wipe. A future archaeologist squeezes between monuments, brushes dust from `FINAL FIX`, and places a respectful museum card beside it: `REMARKABLE. THEY KNEW.` Hold on the archaeologist admiring what was plainly not final.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sandstone #A98D67
Character/prop lock: USER_A: simple angular human pictogram; MONUMENT_BASE: identical stone slab shape cloned for every commit with label changed only; ARCHAEOLOGIST_A: thin bent figure with brush. No monument shape drift.
Reference board: use the canonical sequence reference board for Animation 13.
START FRAME: 13_git_commit_frame_02.png
END FRAME: 13_git_commit_frame_03.png

CUT 03 OF 03 — 10:00–15:00
Time jumps forward through a simple color wipe. A future archaeologist squeezes between monuments, brushes dust from `FINAL FIX`, and places a respectful museum card beside it: `REMARKABLE. THEY KNEW.` Hold on the archaeologist admiring what was plainly not final.
```


---

# Animation 14 — `nmap` — network mapper

## Chapter 13 — Reconnaissance & Security

## Source animation sequence

A nautical chart appears.

Ports numbered `22`, `80`, `443`.

Tiny ships knock.

One harbour answers.

A stamp descends:

**OPEN**

Another rejects the approach:

**CLOSED**

A third disappears behind fog:

**FILTERED**

The Port Authority adds a line beneath all three:

**OBSERVATIONS, NOT INVITATIONS**

## Sequence lock

**Background:** sea green `#1D6B62`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

SHIP_BASE: identical tiny ship pictogram cloned for each port; PORT_22/80/443: identical harbour geometry with numbered signs; PORT_CLERK_A: small official figure. Only port state changes, not design.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
SHIP_BASE: identical tiny ship pictogram cloned for each port; PORT_22/80/443: identical harbour geometry with numbered signs; PORT_CLERK_A: small official figure. Only port state changes, not design.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `14_nmap_frame_00.png`

**Used as:** Cut 01 START

A nautical chart fills the active picture. Three stylized harbours are numbered `22`, `80`, `443`.

### Frame 01 — `14_nmap_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Second ship waiting.

### Frame 02 — `14_nmap_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

The three-port comparison.

### Frame 03 — `14_nmap_frame_03.png`

**Used as:** Cut 03 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `14_nmap_frame_00.png`

**END:** `14_nmap_frame_01.png`

A nautical chart fills the active picture. Three stylized harbours are numbered `22`, `80`, `443`. Tiny ships approach and knock at each port rather than sailing in. One harbour gate opens and receives a stamp `OPEN`. End with second ship waiting.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sea green #1D6B62
Character/prop lock: SHIP_BASE: identical tiny ship pictogram cloned for each port; PORT_22/80/443: identical harbour geometry with numbered signs; PORT_CLERK_A: small official figure. Only port state changes, not design.
Reference board: use the canonical sequence reference board for Animation 14.
START FRAME: 14_nmap_frame_00.png
END FRAME: 14_nmap_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A nautical chart fills the active picture. Three stylized harbours are numbered `22`, `80`, `443`. Tiny ships approach and knock at each port rather than sailing in. One harbour gate opens and receives a stamp `OPEN`. End with second ship waiting.
```

## Cut 02 — 05:00–10:00

**START:** `14_nmap_frame_01.png`

**END:** `14_nmap_frame_02.png`

Second harbour gate shuts firmly and receives `CLOSED`. Third harbour becomes obscured by a flat block of fog or screening shutters; the ship cannot determine what is behind it. Stamp `FILTERED`. Keep all three states visible together. End on the three-port comparison.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sea green #1D6B62
Character/prop lock: SHIP_BASE: identical tiny ship pictogram cloned for each port; PORT_22/80/443: identical harbour geometry with numbered signs; PORT_CLERK_A: small official figure. Only port state changes, not design.
Reference board: use the canonical sequence reference board for Animation 14.
START FRAME: 14_nmap_frame_01.png
END FRAME: 14_nmap_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Second harbour gate shuts firmly and receives `CLOSED`. Third harbour becomes obscured by a flat block of fog or screening shutters; the ship cannot determine what is behind it. Stamp `FILTERED`. Keep all three states visible together. End on the three-port comparison.
```

## Cut 03 — 10:00–15:00

**START:** `14_nmap_frame_02.png`

**END:** `14_nmap_frame_03.png`

A sober Port Authority clerk slides a long footer under the chart: `OBSERVATIONS, NOT INVITATIONS`. The little ship at the OPEN port begins to enter; the clerk raises one finger and the ship reverses half a boat length. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sea green #1D6B62
Character/prop lock: SHIP_BASE: identical tiny ship pictogram cloned for each port; PORT_22/80/443: identical harbour geometry with numbered signs; PORT_CLERK_A: small official figure. Only port state changes, not design.
Reference board: use the canonical sequence reference board for Animation 14.
START FRAME: 14_nmap_frame_02.png
END FRAME: 14_nmap_frame_03.png

CUT 03 OF 03 — 10:00–15:00
A sober Port Authority clerk slides a long footer under the chart: `OBSERVATIONS, NOT INVITATIONS`. The little ship at the OPEN port begins to enter; the clerk raises one finger and the ship reverses half a boat length. Hold.
```


---

# Animation 15 — `crond` / `crontab` — scheduled jobs

## Chapter 14 — Automation

## Source animation sequence

A user writes:

`03:00 — DO THE THING`

The card is filed in a cabinet marked:

`crontab`

A small clockwork clerk marked `crond` checks the cabinet.

03:00.

Lever.

Something enormous happens off-screen.

Next night, an Android bailiff quietly removes the clerk.

03:00.

The card glows expectantly.

Nothing happens.

At 03:07 the clerk returns.

The card points accusingly at the clock.

The clerk points at the bailiff.

## Sequence lock

**Background:** olive `#697442`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

USER_A: angular human pictogram; CROND_CLERK_A: tiny clockwork clerk with circular clock-face head and lever arm; ANDROID_BAILIFF_A: tall rigid figure carrying a rectangular rulebook. These exact characters persist across all four cuts.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
USER_A: angular human pictogram; CROND_CLERK_A: tiny clockwork clerk with circular clock-face head and lever arm; ANDROID_BAILIFF_A: tall rigid figure carrying a rectangular rulebook. These exact characters persist across all four cuts.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `15_crond_frame_00.png`

**Used as:** Cut 01 START

A user writes `03:00 — DO THE THING` on a card and files it into a cabinet labeled `crontab`.

### Frame 01 — `15_crond_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Vibrating room, calm clerk.

### Frame 02 — `15_crond_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

03:00 approaching and no clerk.

### Frame 03 — `15_crond_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

Offended card and untouched lever.

### Frame 04 — `15_crond_frame_04.png`

**Used as:** Cut 04 END

The triangular blame diagram.

## Cut 01 — 00:00–05:00

**START:** `15_crond_frame_00.png`

**END:** `15_crond_frame_01.png`

A user writes `03:00 — DO THE THING` on a card and files it into a cabinet labeled `crontab`. A small clockwork clerk labeled `crond` opens the drawer and checks the card against a wall clock. At exactly 03:00 the clerk pulls a lever; something enormous happens offscreen. End on vibrating room, calm clerk.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: olive #697442
Character/prop lock: USER_A: angular human pictogram; CROND_CLERK_A: tiny clockwork clerk with circular clock-face head and lever arm; ANDROID_BAILIFF_A: tall rigid figure carrying a rectangular rulebook. These exact characters persist across all four cuts.
Reference board: use the canonical sequence reference board for Animation 15.
START FRAME: 15_crond_frame_00.png
END FRAME: 15_crond_frame_01.png

CUT 01 OF 04 — 00:00–05:00
A user writes `03:00 — DO THE THING` on a card and files it into a cabinet labeled `crontab`. A small clockwork clerk labeled `crond` opens the drawer and checks the card against a wall clock. At exactly 03:00 the clerk pulls a lever; something enormous happens offscreen. End on vibrating room, calm clerk.
```

## Cut 02 — 05:00–10:00

**START:** `15_crond_frame_01.png`

**END:** `15_crond_frame_02.png`

Next-night card flips into view. Same cabinet, same clock, same clerk. Before 03:00, an Android bailiff enters silently, picks up the `crond` clerk, and carries it out through a side door. The card remains filed. End with 03:00 approaching and no clerk.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: olive #697442
Character/prop lock: USER_A: angular human pictogram; CROND_CLERK_A: tiny clockwork clerk with circular clock-face head and lever arm; ANDROID_BAILIFF_A: tall rigid figure carrying a rectangular rulebook. These exact characters persist across all four cuts.
Reference board: use the canonical sequence reference board for Animation 15.
START FRAME: 15_crond_frame_01.png
END FRAME: 15_crond_frame_02.png

CUT 02 OF 04 — 05:00–10:00
Next-night card flips into view. Same cabinet, same clock, same clerk. Before 03:00, an Android bailiff enters silently, picks up the `crond` clerk, and carries it out through a side door. The card remains filed. End with 03:00 approaching and no clerk.
```

## Cut 03 — 10:00–15:00

**START:** `15_crond_frame_02.png`

**END:** `15_crond_frame_03.png`

Clock hits 03:00. The instruction card glows expectantly in its drawer. Nothing happens. Let the full remaining beat be still except for the second hand passing 03:00. End on offended card and untouched lever.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: olive #697442
Character/prop lock: USER_A: angular human pictogram; CROND_CLERK_A: tiny clockwork clerk with circular clock-face head and lever arm; ANDROID_BAILIFF_A: tall rigid figure carrying a rectangular rulebook. These exact characters persist across all four cuts.
Reference board: use the canonical sequence reference board for Animation 15.
START FRAME: 15_crond_frame_02.png
END FRAME: 15_crond_frame_03.png

CUT 03 OF 04 — 10:00–15:00
Clock hits 03:00. The instruction card glows expectantly in its drawer. Nothing happens. Let the full remaining beat be still except for the second hand passing 03:00. End on offended card and untouched lever.
```

## Cut 04 — 15:00–20:00

**START:** `15_crond_frame_03.png`

**END:** `15_crond_frame_04.png`

Clock reads 03:07. The bailiff returns the clerk to the desk. The card points accusingly at the clock; the clerk points at the bailiff; the bailiff points at an Android-shaped rulebook. Nobody takes responsibility. Hold the triangular blame diagram.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: olive #697442
Character/prop lock: USER_A: angular human pictogram; CROND_CLERK_A: tiny clockwork clerk with circular clock-face head and lever arm; ANDROID_BAILIFF_A: tall rigid figure carrying a rectangular rulebook. These exact characters persist across all four cuts.
Reference board: use the canonical sequence reference board for Animation 15.
START FRAME: 15_crond_frame_03.png
END FRAME: 15_crond_frame_04.png

CUT 04 OF 04 — 15:00–20:00
Clock reads 03:07. The bailiff returns the clerk to the desk. The card points accusingly at the clock; the clerk points at the bailiff; the bailiff points at an Android-shaped rulebook. Nobody takes responsibility. Hold the triangular blame diagram.
```


---

# Animation 16 — `watch` — watch

## Chapter 14 — Automation

## Source animation sequence

A terminal displays:

`watch something`

A small observer sits in a chair staring only at the command's output.

Tick.

No change.

Tick.

No change.

Behind the observer, the entire room is quietly replaced.

Tick.

The output changes by one character.

The observer rings an enormous bell.

The room replacement goes entirely unreported.

## Sequence lock

**Background:** pale blue `#86A8B8`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

OBSERVER_A: seated narrow figure with round head and enormous bell beside chair; ROOM_PROPS_SET_A: fixed chair, plant, wall sign and table shapes. Observer silhouette never changes while environment swaps.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
OBSERVER_A: seated narrow figure with round head and enormous bell beside chair; ROOM_PROPS_SET_A: fixed chair, plant, wall sign and table shapes. Observer silhouette never changes while environment swaps.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `16_watch_frame_00.png`

**Used as:** Cut 01 START

A terminal panel displays `watch something`. In front of it a small observer sits in a chair staring only at the output.

### Frame 01 — `16_watch_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Stagehands begin moving behind the observer.

### Frame 02 — `16_watch_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

A totally new room.

### Frame 03 — `16_watch_frame_03.png`

**Used as:** Cut 03 END

Observer congratulating themself beneath the new room.

## Cut 01 — 00:00–05:00

**START:** `16_watch_frame_00.png`

**END:** `16_watch_frame_01.png`

A terminal panel displays `watch something`. In front of it a small observer sits in a chair staring only at the output. Tick: no change. Tick: no change. The observer never looks away. End as stagehands begin moving behind the observer.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale blue #86A8B8
Character/prop lock: OBSERVER_A: seated narrow figure with round head and enormous bell beside chair; ROOM_PROPS_SET_A: fixed chair, plant, wall sign and table shapes. Observer silhouette never changes while environment swaps.
Reference board: use the canonical sequence reference board for Animation 16.
START FRAME: 16_watch_frame_00.png
END FRAME: 16_watch_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A terminal panel displays `watch something`. In front of it a small observer sits in a chair staring only at the output. Tick: no change. Tick: no change. The observer never looks away. End as stagehands begin moving behind the observer.
```

## Cut 02 — 05:00–10:00

**START:** `16_watch_frame_01.png`

**END:** `16_watch_frame_02.png`

Behind the observer, the entire room is silently replaced: wall color, furniture, signs, even a plant. Use linear slide swaps like diagram cards. The `watch` output remains unchanged and the observer remains rigidly focused. End with a totally new room.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale blue #86A8B8
Character/prop lock: OBSERVER_A: seated narrow figure with round head and enormous bell beside chair; ROOM_PROPS_SET_A: fixed chair, plant, wall sign and table shapes. Observer silhouette never changes while environment swaps.
Reference board: use the canonical sequence reference board for Animation 16.
START FRAME: 16_watch_frame_01.png
END FRAME: 16_watch_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Behind the observer, the entire room is silently replaced: wall color, furniture, signs, even a plant. Use linear slide swaps like diagram cards. The `watch` output remains unchanged and the observer remains rigidly focused. End with a totally new room.
```

## Cut 03 — 10:00–15:00

**START:** `16_watch_frame_02.png`

**END:** `16_watch_frame_03.png`

Next tick: one character in the command output changes. The observer instantly rings an enormous alarm bell and records the event. The completely replaced room remains unmentioned. Hold on observer congratulating themself beneath the new room.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale blue #86A8B8
Character/prop lock: OBSERVER_A: seated narrow figure with round head and enormous bell beside chair; ROOM_PROPS_SET_A: fixed chair, plant, wall sign and table shapes. Observer silhouette never changes while environment swaps.
Reference board: use the canonical sequence reference board for Animation 16.
START FRAME: 16_watch_frame_02.png
END FRAME: 16_watch_frame_03.png

CUT 03 OF 03 — 10:00–15:00
Next tick: one character in the command output changes. The observer instantly rings an enormous alarm bell and records the event. The completely replaced room remains unmentioned. Hold on observer congratulating themself beneath the new room.
```


---

# Animation 17 — `AI chat` — natural language to command

## Chapter 15 — Artificial Wisdom

## Source animation sequence

A gigantic archive marked **ANSWERS** stretches into the distance.

Every shelf is full.

Beside it is one tiny empty cabinet marked:

**QUESTIONS**

AI enters carrying a question mark.

It places it carefully in the cabinet.

Alarms sound.

Researchers run in.

Confetti falls.

A plaque descends:

**FIRST QUESTION**

The Guide examines it.

It reads:

`pwd`

The Guide looks across at a shelf labelled:

`WHERE YOU ARE`

It has been there for years.

Everyone quietly stops applauding.

## Sequence lock

**Background:** cobalt `#176EAA`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

AI_RESEARCHER_A: narrow humanoid carrying one oversized question-mark object; GUIDE_RESEARCHER_BASE: identical small archive-worker clone; QUESTION_MARK_A: fixed punctuation-shaped prop. Reuse exact figures in every archive shot.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
AI_RESEARCHER_A: narrow humanoid carrying one oversized question-mark object; GUIDE_RESEARCHER_BASE: identical small archive-worker clone; QUESTION_MARK_A: fixed punctuation-shaped prop. Reuse exact figures in every archive shot.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `17_ai_chat_frame_00.png`

**Used as:** Cut 01 START

A vast archive labeled `ANSWERS` stretches deep across the frame, shelves packed edge to edge.

### Frame 01 — `17_ai_chat_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

The researcher standing before the empty cabinet.

### Frame 02 — `17_ai_chat_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Plaque.

### Frame 03 — `17_ai_chat_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

End halfway toward a shelf label.

### Frame 04 — `17_ai_chat_frame_04.png`

**Used as:** Cut 04 END

The embarrassed institution.

## Cut 01 — 00:00–05:00

**START:** `17_ai_chat_frame_00.png`

**END:** `17_ai_chat_frame_01.png`

A vast archive labeled `ANSWERS` stretches deep across the frame, shelves packed edge to edge. Beside it, absurdly small, an empty cabinet labeled `QUESTIONS`. A new AI researcher enters carrying a single large question mark. End with the researcher standing before the empty cabinet.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: cobalt #176EAA
Character/prop lock: AI_RESEARCHER_A: narrow humanoid carrying one oversized question-mark object; GUIDE_RESEARCHER_BASE: identical small archive-worker clone; QUESTION_MARK_A: fixed punctuation-shaped prop. Reuse exact figures in every archive shot.
Reference board: use the canonical sequence reference board for Animation 17.
START FRAME: 17_ai_chat_frame_00.png
END FRAME: 17_ai_chat_frame_01.png

CUT 01 OF 04 — 00:00–05:00
A vast archive labeled `ANSWERS` stretches deep across the frame, shelves packed edge to edge. Beside it, absurdly small, an empty cabinet labeled `QUESTIONS`. A new AI researcher enters carrying a single large question mark. End with the researcher standing before the empty cabinet.
```

## Cut 02 — 05:00–10:00

**START:** `17_ai_chat_frame_01.png`

**END:** `17_ai_chat_frame_02.png`

AI carefully places the question mark inside. Alarms flash as solid color panels; researchers rush in from multiple directions; tiny confetti shapes fall. A plaque descends reading `FIRST QUESTION`. No character celebrates emotionally; the institution does it mechanically. End on plaque.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: cobalt #176EAA
Character/prop lock: AI_RESEARCHER_A: narrow humanoid carrying one oversized question-mark object; GUIDE_RESEARCHER_BASE: identical small archive-worker clone; QUESTION_MARK_A: fixed punctuation-shaped prop. Reuse exact figures in every archive shot.
Reference board: use the canonical sequence reference board for Animation 17.
START FRAME: 17_ai_chat_frame_01.png
END FRAME: 17_ai_chat_frame_02.png

CUT 02 OF 04 — 05:00–10:00
AI carefully places the question mark inside. Alarms flash as solid color panels; researchers rush in from multiple directions; tiny confetti shapes fall. A plaque descends reading `FIRST QUESTION`. No character celebrates emotionally; the institution does it mechanically. End on plaque.
```

## Cut 03 — 10:00–15:00

**START:** `17_ai_chat_frame_02.png`

**END:** `17_ai_chat_frame_03.png`

The Guide opens the cabinet and inspects the historic first question. It is simply `pwd`. The celebration freezes. Camera begins a slow flat pan across the adjacent ANSWERS archive. End halfway toward a shelf label.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: cobalt #176EAA
Character/prop lock: AI_RESEARCHER_A: narrow humanoid carrying one oversized question-mark object; GUIDE_RESEARCHER_BASE: identical small archive-worker clone; QUESTION_MARK_A: fixed punctuation-shaped prop. Reuse exact figures in every archive shot.
Reference board: use the canonical sequence reference board for Animation 17.
START FRAME: 17_ai_chat_frame_02.png
END FRAME: 17_ai_chat_frame_03.png

CUT 03 OF 04 — 10:00–15:00
The Guide opens the cabinet and inspects the historic first question. It is simply `pwd`. The celebration freezes. Camera begins a slow flat pan across the adjacent ANSWERS archive. End halfway toward a shelf label.
```

## Cut 04 — 15:00–20:00

**START:** `17_ai_chat_frame_03.png`

**END:** `17_ai_chat_frame_04.png`

Pan lands on a shelf labeled `WHERE YOU ARE`, packed and visibly old, dust included. Everyone turns from the new `pwd` question to the ancient answer shelf. Confetti stops mid-fall and drops straight down. Hold on the embarrassed institution.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: cobalt #176EAA
Character/prop lock: AI_RESEARCHER_A: narrow humanoid carrying one oversized question-mark object; GUIDE_RESEARCHER_BASE: identical small archive-worker clone; QUESTION_MARK_A: fixed punctuation-shaped prop. Reuse exact figures in every archive shot.
Reference board: use the canonical sequence reference board for Animation 17.
START FRAME: 17_ai_chat_frame_03.png
END FRAME: 17_ai_chat_frame_04.png

CUT 04 OF 04 — 15:00–20:00
Pan lands on a shelf labeled `WHERE YOU ARE`, packed and visibly old, dust included. Everyone turns from the new `pwd` question to the ancient answer shelf. Confetti stops mid-fall and drops straight down. Hold on the embarrassed institution.
```


---

# Animation 18 — `skill` — installed skill package

## Chapter 15 — Artificial Wisdom

## Source animation sequence

A researcher spends years studying an enormous subject.

Books pile up.

Maps.

Charts.

Field notes.

Eventually the researcher compresses all of it into a tiny package marked:

`skill`

The package is installed.

An assistant opens it.

A miniature version of the researcher climbs out, unfolds a desk, and begins whispering instructions into the assistant's ear.

A question arrives.

The assistant answers.

The tiny researcher looks pleased.

Another question arrives on an unrelated subject.

The assistant turns toward the researcher.

The researcher immediately closes the desk and pretends to be luggage.

## Sequence lock

**Background:** dusty cyan `#638D91`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

RESEARCHER_A: tall slim scholar with round glasses shape and rectangular torso; ASSISTANT_A: broad simple assistant silhouette; MINI_RESEARCHER_A is an exact 20%-scale clone of RESEARCHER_A, not a new design; SKILL_PACKAGE_A fixed small luggage shape.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
RESEARCHER_A: tall slim scholar with round glasses shape and rectangular torso; ASSISTANT_A: broad simple assistant silhouette; MINI_RESEARCHER_A is an exact 20%-scale clone of RESEARCHER_A, not a new design; SKILL_PACKAGE_A fixed small luggage shape.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `18_skill_frame_00.png`

**Used as:** Cut 01 START

A researcher studies at a desk while years pass through quick graphic day/night cards. Books pile up, then maps, charts and field notes accumulate around the figure until the desk is nearly buried.

### Frame 01 — `18_skill_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Towering knowledge pile.

### Frame 02 — `18_skill_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Package.

### Frame 03 — `18_skill_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

End there.

### Frame 04 — `18_skill_frame_04.png`

**Used as:** Cut 04 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `18_skill_frame_00.png`

**END:** `18_skill_frame_01.png`

A researcher studies at a desk while years pass through quick graphic day/night cards. Books pile up, then maps, charts and field notes accumulate around the figure until the desk is nearly buried. End on towering knowledge pile.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty cyan #638D91
Character/prop lock: RESEARCHER_A: tall slim scholar with round glasses shape and rectangular torso; ASSISTANT_A: broad simple assistant silhouette; MINI_RESEARCHER_A is an exact 20%-scale clone of RESEARCHER_A, not a new design; SKILL_PACKAGE_A fixed small luggage shape.
Reference board: use the canonical sequence reference board for Animation 18.
START FRAME: 18_skill_frame_00.png
END FRAME: 18_skill_frame_01.png

CUT 01 OF 04 — 00:00–05:00
A researcher studies at a desk while years pass through quick graphic day/night cards. Books pile up, then maps, charts and field notes accumulate around the figure until the desk is nearly buried. End on towering knowledge pile.
```

## Cut 02 — 05:00–10:00

**START:** `18_skill_frame_01.png`

**END:** `18_skill_frame_02.png`

The researcher feeds books, maps, charts and notes into a geometric compression machine. The huge pile concertinas smaller and smaller until a tiny sealed package emerges labeled `skill`. The researcher stares at the package that now contains years. End on package.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty cyan #638D91
Character/prop lock: RESEARCHER_A: tall slim scholar with round glasses shape and rectangular torso; ASSISTANT_A: broad simple assistant silhouette; MINI_RESEARCHER_A is an exact 20%-scale clone of RESEARCHER_A, not a new design; SKILL_PACKAGE_A fixed small luggage shape.
Reference board: use the canonical sequence reference board for Animation 18.
START FRAME: 18_skill_frame_01.png
END FRAME: 18_skill_frame_02.png

CUT 02 OF 04 — 05:00–10:00
The researcher feeds books, maps, charts and notes into a geometric compression machine. The huge pile concertinas smaller and smaller until a tiny sealed package emerges labeled `skill`. The researcher stares at the package that now contains years. End on package.
```

## Cut 03 — 10:00–15:00

**START:** `18_skill_frame_02.png`

**END:** `18_skill_frame_03.png`

The `skill` package is installed into an assistant. It opens like luggage; a miniature researcher climbs out, unfolds a tiny desk on the assistant's shoulder and whispers instructions. A relevant question card arrives; assistant answers correctly. Tiny researcher looks satisfied. End there.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty cyan #638D91
Character/prop lock: RESEARCHER_A: tall slim scholar with round glasses shape and rectangular torso; ASSISTANT_A: broad simple assistant silhouette; MINI_RESEARCHER_A is an exact 20%-scale clone of RESEARCHER_A, not a new design; SKILL_PACKAGE_A fixed small luggage shape.
Reference board: use the canonical sequence reference board for Animation 18.
START FRAME: 18_skill_frame_02.png
END FRAME: 18_skill_frame_03.png

CUT 03 OF 04 — 10:00–15:00
The `skill` package is installed into an assistant. It opens like luggage; a miniature researcher climbs out, unfolds a tiny desk on the assistant's shoulder and whispers instructions. A relevant question card arrives; assistant answers correctly. Tiny researcher looks satisfied. End there.
```

## Cut 04 — 15:00–20:00

**START:** `18_skill_frame_03.png`

**END:** `18_skill_frame_04.png`

A second question card arrives with an obviously unrelated symbol. The assistant turns expectantly to the miniature researcher. The researcher instantly folds the desk, climbs into the package, shuts it, and arranges the package to look exactly like ordinary luggage. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty cyan #638D91
Character/prop lock: RESEARCHER_A: tall slim scholar with round glasses shape and rectangular torso; ASSISTANT_A: broad simple assistant silhouette; MINI_RESEARCHER_A is an exact 20%-scale clone of RESEARCHER_A, not a new design; SKILL_PACKAGE_A fixed small luggage shape.
Reference board: use the canonical sequence reference board for Animation 18.
START FRAME: 18_skill_frame_03.png
END FRAME: 18_skill_frame_04.png

CUT 04 OF 04 — 15:00–20:00
A second question card arrives with an obviously unrelated symbol. The assistant turns expectantly to the miniature researcher. The researcher instantly folds the desk, climbs into the package, shuts it, and arranges the package to look exactly like ordinary luggage. Hold.
```


---

# Animation 19 — `ls -a` — list all

## chapter_01 — The Absurdity of Redundancy

## Source animation sequence

A row of ordinary files stands for inspection.

Behind them, several dotfiles stand perfectly still in plain sight.

`ls`

The inspector walks past without acknowledging them.

One dotfile waves.

Nothing.

`ls -a`

The inspector suddenly turns.

The dotfiles freeze.

A spotlight appears.

At the end of the line, the directory itself raises a small card:

`.`

Behind it, its parent leans through the doorway holding:

`..`

The inspector quietly puts the clipboard down.

## Sequence lock

**Background:** dark navy `#12243A`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

FILE_BASE: identical upright file-card character; DOTFILE_BASE: exact clone of FILE_BASE with a small dot mark; INSPECTOR_A: thin figure with clipboard; DIRECTORY_DOT and DIRECTORY_DOTDOT are fixed card props. Preserve clones.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
FILE_BASE: identical upright file-card character; DOTFILE_BASE: exact clone of FILE_BASE with a small dot mark; INSPECTOR_A: thin figure with clipboard; DIRECTORY_DOT and DIRECTORY_DOTDOT are fixed card props. Preserve clones.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `19_ls_a_frame_00.png`

**Used as:** Cut 01 START

A row of ordinary file characters stands for inspection. Several dotfiles stand directly behind them in plain sight, completely still.

### Frame 01 — `19_ls_a_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Inspector ignoring it.

### Frame 02 — `19_ls_a_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

The newly acknowledged hidden files.

### Frame 03 — `19_ls_a_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

Clipboard lowering.

### Frame 04 — `19_ls_a_frame_04.png`

**Used as:** Cut 04 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `19_ls_a_frame_00.png`

**END:** `19_ls_a_frame_01.png`

A row of ordinary file characters stands for inspection. Several dotfiles stand directly behind them in plain sight, completely still. An inspector walks along after the command `ls`, checking only ordinary files and deliberately looking through the dotfiles. One dotfile waves. End with inspector ignoring it.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #12243A
Character/prop lock: FILE_BASE: identical upright file-card character; DOTFILE_BASE: exact clone of FILE_BASE with a small dot mark; INSPECTOR_A: thin figure with clipboard; DIRECTORY_DOT and DIRECTORY_DOTDOT are fixed card props. Preserve clones.
Reference board: use the canonical sequence reference board for Animation 19.
START FRAME: 19_ls_a_frame_00.png
END FRAME: 19_ls_a_frame_01.png

CUT 01 OF 04 — 00:00–05:00
A row of ordinary file characters stands for inspection. Several dotfiles stand directly behind them in plain sight, completely still. An inspector walks along after the command `ls`, checking only ordinary files and deliberately looking through the dotfiles. One dotfile waves. End with inspector ignoring it.
```

## Cut 02 — 05:00–10:00

**START:** `19_ls_a_frame_01.png`

**END:** `19_ls_a_frame_02.png`

`ls -a` replaces the command label. The inspector abruptly turns around. Every dotfile freezes in place like children caught awake. A hard flat spotlight snaps on over them. End on the newly acknowledged hidden files.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #12243A
Character/prop lock: FILE_BASE: identical upright file-card character; DOTFILE_BASE: exact clone of FILE_BASE with a small dot mark; INSPECTOR_A: thin figure with clipboard; DIRECTORY_DOT and DIRECTORY_DOTDOT are fixed card props. Preserve clones.
Reference board: use the canonical sequence reference board for Animation 19.
START FRAME: 19_ls_a_frame_01.png
END FRAME: 19_ls_a_frame_02.png

CUT 02 OF 04 — 05:00–10:00
`ls -a` replaces the command label. The inspector abruptly turns around. Every dotfile freezes in place like children caught awake. A hard flat spotlight snaps on over them. End on the newly acknowledged hidden files.
```

## Cut 03 — 10:00–15:00

**START:** `19_ls_a_frame_02.png`

**END:** `19_ls_a_frame_03.png`

At the end of the line, the directory itself raises a small card `.`. Behind it, a larger parent directory leans through a doorway holding `..`. The inspector looks from `.` to `..` to the clipboard, clearly reconsidering what `all` has done. End on clipboard lowering.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #12243A
Character/prop lock: FILE_BASE: identical upright file-card character; DOTFILE_BASE: exact clone of FILE_BASE with a small dot mark; INSPECTOR_A: thin figure with clipboard; DIRECTORY_DOT and DIRECTORY_DOTDOT are fixed card props. Preserve clones.
Reference board: use the canonical sequence reference board for Animation 19.
START FRAME: 19_ls_a_frame_02.png
END FRAME: 19_ls_a_frame_03.png

CUT 03 OF 04 — 10:00–15:00
At the end of the line, the directory itself raises a small card `.`. Behind it, a larger parent directory leans through a doorway holding `..`. The inspector looks from `.` to `..` to the clipboard, clearly reconsidering what `all` has done. End on clipboard lowering.
```

## Cut 04 — 15:00–20:00

**START:** `19_ls_a_frame_03.png`

**END:** `19_ls_a_frame_04.png`

The inspector quietly sets the clipboard on the floor and walks away while `.` and `..` remain in line with the dotfiles. One dotfile starts to wave again, then thinks better of it. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #12243A
Character/prop lock: FILE_BASE: identical upright file-card character; DOTFILE_BASE: exact clone of FILE_BASE with a small dot mark; INSPECTOR_A: thin figure with clipboard; DIRECTORY_DOT and DIRECTORY_DOTDOT are fixed card props. Preserve clones.
Reference board: use the canonical sequence reference board for Animation 19.
START FRAME: 19_ls_a_frame_03.png
END FRAME: 19_ls_a_frame_04.png

CUT 04 OF 04 — 15:00–20:00
The inspector quietly sets the clipboard on the floor and walks away while `.` and `..` remain in line with the dotfiles. One dotfile starts to wave again, then thinks better of it. Hold.
```


---

# Animation 20 — `sudo` — run with another user's privileges

## chapter_02 — Doppelgänger Permissions

## Source animation sequence

A user stands before a locked administrative door.

They submit:

`sudo command`

A clerk pins a temporary badge to **command**.

The command passes through.

The user tries to follow.

The clerk checks the badge.

It is attached to the command.

On an unrooted device a second door appears behind the first:

**ROOT AUTHORITY NOT PRESENT**

The first clerk closes the service window.

## Sequence lock

**Background:** dusty pink `#B57B86`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

USER_A: angular human pictogram; CLERK_A: narrow service-window official; COMMAND_A: literal rectangular word-object that receives the authorization badge; BADGE_A fixed capsule badge. Privilege changes props, never character design.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
USER_A: angular human pictogram; CLERK_A: narrow service-window official; COMMAND_A: literal rectangular word-object that receives the authorization badge; BADGE_A fixed capsule badge. Privilege changes props, never character design.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `20_sudo_frame_00.png`

**Used as:** Cut 01 START

A user stands before a locked administrative door and submits `sudo command` through a service window.

### Frame 01 — `20_sudo_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

User stepping forward to follow.

### Frame 02 — `20_sudo_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Another door begins sliding into place behind the first.

### Frame 03 — `20_sudo_frame_03.png`

**Used as:** Cut 03 END

Two doors and one temporarily privileged verb going nowhere.

## Cut 01 — 00:00–05:00

**START:** `20_sudo_frame_00.png`

**END:** `20_sudo_frame_01.png`

A user stands before a locked administrative door and submits `sudo command` through a service window. A clerk pins a temporary authorization badge directly onto the word/object `command`, not onto the user. The command passes through the door. End with user stepping forward to follow.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty pink #B57B86
Character/prop lock: USER_A: angular human pictogram; CLERK_A: narrow service-window official; COMMAND_A: literal rectangular word-object that receives the authorization badge; BADGE_A fixed capsule badge. Privilege changes props, never character design.
Reference board: use the canonical sequence reference board for Animation 20.
START FRAME: 20_sudo_frame_00.png
END FRAME: 20_sudo_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A user stands before a locked administrative door and submits `sudo command` through a service window. A clerk pins a temporary authorization badge directly onto the word/object `command`, not onto the user. The command passes through the door. End with user stepping forward to follow.
```

## Cut 02 — 05:00–10:00

**START:** `20_sudo_frame_01.png`

**END:** `20_sudo_frame_02.png`

The clerk blocks the user, checks for a badge, then points through the door at the badge still attached to the command. The user points at themself; clerk points at the grammar of the request. Keep it bureaucratic and literal. End as another door begins sliding into place behind the first.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty pink #B57B86
Character/prop lock: USER_A: angular human pictogram; CLERK_A: narrow service-window official; COMMAND_A: literal rectangular word-object that receives the authorization badge; BADGE_A fixed capsule badge. Privilege changes props, never character design.
Reference board: use the canonical sequence reference board for Animation 20.
START FRAME: 20_sudo_frame_01.png
END FRAME: 20_sudo_frame_02.png

CUT 02 OF 03 — 05:00–10:00
The clerk blocks the user, checks for a badge, then points through the door at the badge still attached to the command. The user points at themself; clerk points at the grammar of the request. Keep it bureaucratic and literal. End as another door begins sliding into place behind the first.
```

## Cut 03 — 10:00–15:00

**START:** `20_sudo_frame_02.png`

**END:** `20_sudo_frame_03.png`

On an unrooted-device variant, the second door is now fully visible: `ROOT AUTHORITY NOT PRESENT`. The authorized command reaches it and cannot continue. The first clerk calmly closes the service window. Hold on two doors and one temporarily privileged verb going nowhere.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty pink #B57B86
Character/prop lock: USER_A: angular human pictogram; CLERK_A: narrow service-window official; COMMAND_A: literal rectangular word-object that receives the authorization badge; BADGE_A fixed capsule badge. Privilege changes props, never character design.
Reference board: use the canonical sequence reference board for Animation 20.
START FRAME: 20_sudo_frame_02.png
END FRAME: 20_sudo_frame_03.png

CUT 03 OF 03 — 10:00–15:00
On an unrooted-device variant, the second door is now fully visible: `ROOT AUTHORITY NOT PRESENT`. The authorized command reaches it and cannot continue. The first clerk calmly closes the service window. Hold on two doors and one temporarily privileged verb going nowhere.
```


---

# Animation 21 — `rmdir` — remove directory

## chapter_03 — The Infinite Directory

## Source animation sequence

A tiny hotel is marked:

`EMPTY DIRECTORY`

A demolition officer checks rooms.

Room 1: empty.

Room 2: empty.

Lobby: a figure labelled `.` points at itself.

Upstairs: another labelled `..` points out the window toward a larger hotel.

The officer studies the regulations.

A footnote reads:

**THESE DON'T COUNT.**

Both figures look offended.

The hotel vanishes.

`..` is left pointing at nothing for half a second, then hurriedly points somewhere else.

## Sequence lock

**Background:** olive green `#69784A`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

OFFICER_A: compact demolition officer pictogram with rectangular rulebook; DOT_A and DOTDOT_A: two simple human-like marker figures, DOT_A shorter and DOTDOT_A taller; HOTEL_A fixed block structure. Preserve exact figures until removal.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
OFFICER_A: compact demolition officer pictogram with rectangular rulebook; DOT_A and DOTDOT_A: two simple human-like marker figures, DOT_A shorter and DOTDOT_A taller; HOTEL_A fixed block structure. Preserve exact figures until removal.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `21_rmdir_frame_00.png`

**Used as:** Cut 01 START

A tiny hotel labeled `EMPTY DIRECTORY`. A demolition officer checks Room 1: empty; Room 2: empty.

### Frame 01 — `21_rmdir_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Officer consulting rulebook.

### Frame 02 — `21_rmdir_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Officer raising a demolition stamp.

### Frame 03 — `21_rmdir_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

`..` still pointing into absence.

### Frame 04 — `21_rmdir_frame_04.png`

**Used as:** Cut 04 END

Repaired hierarchy.

## Cut 01 — 00:00–05:00

**START:** `21_rmdir_frame_00.png`

**END:** `21_rmdir_frame_01.png`

A tiny hotel labeled `EMPTY DIRECTORY`. A demolition officer checks Room 1: empty; Room 2: empty. In the lobby stands a figure labeled `.`, pointing at itself. Upstairs, `..` points out a window toward a larger parent hotel. End with officer consulting rulebook.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: olive green #69784A
Character/prop lock: OFFICER_A: compact demolition officer pictogram with rectangular rulebook; DOT_A and DOTDOT_A: two simple human-like marker figures, DOT_A shorter and DOTDOT_A taller; HOTEL_A fixed block structure. Preserve exact figures until removal.
Reference board: use the canonical sequence reference board for Animation 21.
START FRAME: 21_rmdir_frame_00.png
END FRAME: 21_rmdir_frame_01.png

CUT 01 OF 04 — 00:00–05:00
A tiny hotel labeled `EMPTY DIRECTORY`. A demolition officer checks Room 1: empty; Room 2: empty. In the lobby stands a figure labeled `.`, pointing at itself. Upstairs, `..` points out a window toward a larger parent hotel. End with officer consulting rulebook.
```

## Cut 02 — 05:00–10:00

**START:** `21_rmdir_frame_01.png`

**END:** `21_rmdir_frame_02.png`

Rulebook footnote enlarges on screen: `THESE DON'T COUNT.` The officer looks back at `.` and `..`. Both figures cross their arms or otherwise register bureaucratic offense without facial animation. End with officer raising a demolition stamp.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: olive green #69784A
Character/prop lock: OFFICER_A: compact demolition officer pictogram with rectangular rulebook; DOT_A and DOTDOT_A: two simple human-like marker figures, DOT_A shorter and DOTDOT_A taller; HOTEL_A fixed block structure. Preserve exact figures until removal.
Reference board: use the canonical sequence reference board for Animation 21.
START FRAME: 21_rmdir_frame_01.png
END FRAME: 21_rmdir_frame_02.png

CUT 02 OF 04 — 05:00–10:00
Rulebook footnote enlarges on screen: `THESE DON'T COUNT.` The officer looks back at `.` and `..`. Both figures cross their arms or otherwise register bureaucratic offense without facial animation. End with officer raising a demolition stamp.
```

## Cut 03 — 10:00–15:00

**START:** `21_rmdir_frame_02.png`

**END:** `21_rmdir_frame_03.png`

The officer stamps `EMPTY`. The hotel collapses not explosively but by being cleanly removed from the diagram, leaving `.` gone with it. `..` remains for half a second pointing at the former location. End on `..` still pointing into absence.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: olive green #69784A
Character/prop lock: OFFICER_A: compact demolition officer pictogram with rectangular rulebook; DOT_A and DOTDOT_A: two simple human-like marker figures, DOT_A shorter and DOTDOT_A taller; HOTEL_A fixed block structure. Preserve exact figures until removal.
Reference board: use the canonical sequence reference board for Animation 21.
START FRAME: 21_rmdir_frame_02.png
END FRAME: 21_rmdir_frame_03.png

CUT 03 OF 04 — 10:00–15:00
The officer stamps `EMPTY`. The hotel collapses not explosively but by being cleanly removed from the diagram, leaving `.` gone with it. `..` remains for half a second pointing at the former location. End on `..` still pointing into absence.
```

## Cut 04 — 15:00–20:00

**START:** `21_rmdir_frame_03.png`

**END:** `21_rmdir_frame_04.png`

`..` looks at its own pointing arm, hurriedly swivels and points toward the larger parent hotel instead. The parent hotel accepts the gesture with complete indifference. Hold on repaired hierarchy.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: olive green #69784A
Character/prop lock: OFFICER_A: compact demolition officer pictogram with rectangular rulebook; DOT_A and DOTDOT_A: two simple human-like marker figures, DOT_A shorter and DOTDOT_A taller; HOTEL_A fixed block structure. Preserve exact figures until removal.
Reference board: use the canonical sequence reference board for Animation 21.
START FRAME: 21_rmdir_frame_03.png
END FRAME: 21_rmdir_frame_04.png

CUT 04 OF 04 — 15:00–20:00
`..` looks at its own pointing arm, hurriedly swivels and points toward the larger parent hotel instead. The parent hotel accepts the gesture with complete indifference. Hold on repaired hierarchy.
```


---

# Animation 22 — `find` — find

## chapter_05 — The Anomaly

## Source animation sequence

A detective sits behind a desk.

A distraught editor says:

**"Chapter 5 is missing."**

The detective opens a notebook.

**"Where should I look?"**

The editor answers.

**"What is it named?"**

The editor answers.

**"File type?"**

Answer.

**"Modification time?"**

Answer.

**"Size?"**

Answer.

The detective closes the notebook, reaches under the desk and produces a file.

`chapter_05`

The editor says:

**"That's not it."**

The detective circles the exact search criteria in red.

## Sequence lock

**Background:** oxblood `#781F2E`

**Total duration:** 20 seconds (4 × 5-second cuts)

**Unique boundary frames:** 5

## Character / prop lock

DETECTIVE_A: seated geometric detective with long rectangular notebook and small brimmed hat; EDITOR_A: narrow worried editor with square spectacles; CHAPTER_05_FILE_A fixed file-card. Reuse exact detective/editor designs in every cut.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
DETECTIVE_A: seated geometric detective with long rectangular notebook and small brimmed hat; EDITOR_A: narrow worried editor with square spectacles; CHAPTER_05_FILE_A fixed file-card. Reuse exact detective/editor designs in every cut.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `22_find_frame_00.png`

**Used as:** Cut 01 START

Detective office. A distraught editor presents `CHAPTER 5 IS MISSING`.

### Frame 01 — `22_find_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Detective's notebook filling.

### Frame 02 — `22_find_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Notebook snapping shut.

### Frame 03 — `22_find_frame_03.png`

**Used as:** Cut 03 END / Cut 04 START

The editor stares, then raises a simple card: `THAT'S NOT IT.` End on detective looking at the card, not the editor.

### Frame 04 — `22_find_frame_04.png`

**Used as:** Cut 04 END

Technically closed case.

## Cut 01 — 00:00–05:00

**START:** `22_find_frame_00.png`

**END:** `22_find_frame_01.png`

Detective office. A distraught editor presents `CHAPTER 5 IS MISSING`. Detective opens notebook and asks in rapid labeled cards: `WHERE SHOULD I LOOK?` then `WHAT IS IT NAMED?` The editor supplies answers on small slips. End with detective's notebook filling.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: oxblood #781F2E
Character/prop lock: DETECTIVE_A: seated geometric detective with long rectangular notebook and small brimmed hat; EDITOR_A: narrow worried editor with square spectacles; CHAPTER_05_FILE_A fixed file-card. Reuse exact detective/editor designs in every cut.
Reference board: use the canonical sequence reference board for Animation 22.
START FRAME: 22_find_frame_00.png
END FRAME: 22_find_frame_01.png

CUT 01 OF 04 — 00:00–05:00
Detective office. A distraught editor presents `CHAPTER 5 IS MISSING`. Detective opens notebook and asks in rapid labeled cards: `WHERE SHOULD I LOOK?` then `WHAT IS IT NAMED?` The editor supplies answers on small slips. End with detective's notebook filling.
```

## Cut 02 — 05:00–10:00

**START:** `22_find_frame_01.png`

**END:** `22_find_frame_02.png`

Continue interrogation cards: `FILE TYPE?`, `MODIFICATION TIME?`, `SIZE?`. Each answer is filed precisely into the notebook. With every extra criterion the detective becomes more confident and the editor more boxed in by their own specifications. End with notebook snapping shut.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: oxblood #781F2E
Character/prop lock: DETECTIVE_A: seated geometric detective with long rectangular notebook and small brimmed hat; EDITOR_A: narrow worried editor with square spectacles; CHAPTER_05_FILE_A fixed file-card. Reuse exact detective/editor designs in every cut.
Reference board: use the canonical sequence reference board for Animation 22.
START FRAME: 22_find_frame_01.png
END FRAME: 22_find_frame_02.png

CUT 02 OF 04 — 05:00–10:00
Continue interrogation cards: `FILE TYPE?`, `MODIFICATION TIME?`, `SIZE?`. Each answer is filed precisely into the notebook. With every extra criterion the detective becomes more confident and the editor more boxed in by their own specifications. End with notebook snapping shut.
```

## Cut 03 — 10:00–15:00

**START:** `22_find_frame_02.png`

**END:** `22_find_frame_03.png`

The detective reaches under the desk and immediately produces a file labeled `chapter_05`, exactly matching the supplied criteria. The editor stares, then raises a simple card: `THAT'S NOT IT.` End on detective looking at the card, not the editor.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: oxblood #781F2E
Character/prop lock: DETECTIVE_A: seated geometric detective with long rectangular notebook and small brimmed hat; EDITOR_A: narrow worried editor with square spectacles; CHAPTER_05_FILE_A fixed file-card. Reuse exact detective/editor designs in every cut.
Reference board: use the canonical sequence reference board for Animation 22.
START FRAME: 22_find_frame_02.png
END FRAME: 22_find_frame_03.png

CUT 03 OF 04 — 10:00–15:00
The detective reaches under the desk and immediately produces a file labeled `chapter_05`, exactly matching the supplied criteria. The editor stares, then raises a simple card: `THAT'S NOT IT.` End on detective looking at the card, not the editor.
```

## Cut 04 — 15:00–20:00

**START:** `22_find_frame_03.png`

**END:** `22_find_frame_04.png`

Detective opens notebook, circles every exact search criterion in red, then circles `chapter_05` with the same red pencil. A case stamp descends: `FOUND`. The editor remains unsatisfied. Hold on technically closed case.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: oxblood #781F2E
Character/prop lock: DETECTIVE_A: seated geometric detective with long rectangular notebook and small brimmed hat; EDITOR_A: narrow worried editor with square spectacles; CHAPTER_05_FILE_A fixed file-card. Reuse exact detective/editor designs in every cut.
Reference board: use the canonical sequence reference board for Animation 22.
START FRAME: 22_find_frame_03.png
END FRAME: 22_find_frame_04.png

CUT 04 OF 04 — 15:00–20:00
Detective opens notebook, circles every exact search criterion in red, then circles `chapter_05` with the same red pencil. A case stamp descends: `FOUND`. The editor remains unsatisfied. Hold on technically closed case.
```


---

# Animation 23 — `cat` — concatenate

## chapter_06 — The Mirror Editor

## Source animation sequence

Two files approach a machine marked:

`CONCATENATE`

They enter separately.

One continuous ribbon of text comes out.

Then a single file approaches.

It hesitates.

The machine gestures impatiently.

The file enters.

The same file comes out.

A committee in the corner erupts into furious debate.

The machine prints the committee minutes as one enormous uninterrupted sheet.

## Sequence lock

**Background:** cobalt blue `#235A8E`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

FILE_A and FILE_B: identical file-card shapes distinguished by label only; MACHINE_A: fixed concatenate machine with one intake and ribbon outlet; COMMITTEE_BASE: tiny seated figure cloned for committee. No shape changes.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
FILE_A and FILE_B: identical file-card shapes distinguished by label only; MACHINE_A: fixed concatenate machine with one intake and ribbon outlet; COMMITTEE_BASE: tiny seated figure cloned for committee. No shape changes.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `23_cat_frame_00.png`

**Used as:** Cut 01 START

Two file cards approach a large machine labeled `CONCATENATE`. They enter separately from two slots.

### Frame 01 — `23_cat_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Ribbon continuing offscreen.

### Frame 02 — `23_cat_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Committee gesturing at unchanged file.

### Frame 03 — `23_cat_frame_03.png`

**Used as:** Cut 03 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `23_cat_frame_00.png`

**END:** `23_cat_frame_01.png`

Two file cards approach a large machine labeled `CONCATENATE`. They enter separately from two slots. One continuous ribbon of text exits the other side containing both files in sequence. Make the join visually obvious. End with ribbon continuing offscreen.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: cobalt blue #235A8E
Character/prop lock: FILE_A and FILE_B: identical file-card shapes distinguished by label only; MACHINE_A: fixed concatenate machine with one intake and ribbon outlet; COMMITTEE_BASE: tiny seated figure cloned for committee. No shape changes.
Reference board: use the canonical sequence reference board for Animation 23.
START FRAME: 23_cat_frame_00.png
END FRAME: 23_cat_frame_01.png

CUT 01 OF 03 — 00:00–05:00
Two file cards approach a large machine labeled `CONCATENATE`. They enter separately from two slots. One continuous ribbon of text exits the other side containing both files in sequence. Make the join visually obvious. End with ribbon continuing offscreen.
```

## Cut 02 — 05:00–10:00

**START:** `23_cat_frame_01.png`

**END:** `23_cat_frame_02.png`

Now one single file approaches the machine alone and hesitates. The machine makes an impatient mechanical gesture and pulls it in anyway. The exact same single file comes out unchanged. A committee seated nearby instantly begins furious procedural debate. End on committee gesturing at unchanged file.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: cobalt blue #235A8E
Character/prop lock: FILE_A and FILE_B: identical file-card shapes distinguished by label only; MACHINE_A: fixed concatenate machine with one intake and ribbon outlet; COMMITTEE_BASE: tiny seated figure cloned for committee. No shape changes.
Reference board: use the canonical sequence reference board for Animation 23.
START FRAME: 23_cat_frame_01.png
END FRAME: 23_cat_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Now one single file approaches the machine alone and hesitates. The machine makes an impatient mechanical gesture and pulls it in anyway. The exact same single file comes out unchanged. A committee seated nearby instantly begins furious procedural debate. End on committee gesturing at unchanged file.
```

## Cut 03 — 10:00–15:00

**START:** `23_cat_frame_02.png`

**END:** `23_cat_frame_03.png`

The machine sucks in the committee's meeting notes as they argue and prints them as one enormous uninterrupted sheet with no separators. The sheet rolls across the committee table and out of frame. The committee loses track of where the first meeting ended. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: cobalt blue #235A8E
Character/prop lock: FILE_A and FILE_B: identical file-card shapes distinguished by label only; MACHINE_A: fixed concatenate machine with one intake and ribbon outlet; COMMITTEE_BASE: tiny seated figure cloned for committee. No shape changes.
Reference board: use the canonical sequence reference board for Animation 23.
START FRAME: 23_cat_frame_02.png
END FRAME: 23_cat_frame_03.png

CUT 03 OF 03 — 10:00–15:00
The machine sucks in the committee's meeting notes as they argue and prints them as one enormous uninterrupted sheet with no separators. The sheet rolls across the committee table and out of frame. The committee loses track of where the first meeting ended. Hold.
```


---

# Animation 24 — `ps` — process status

## chapter_07 — Zombie Processes

## Source animation sequence

A crowded station of little processes freezes.

Camera flash.

`ps`

A photograph drops out.

The live station immediately resumes.

Several processes leave.

New ones arrive.

One catches fire.

The user studies the photograph and points confidently at a process which is no longer there.

Behind them, the station has become completely different.

Caption:

**CURRENT PROCESSES**

A small pencil writes underneath:

**at the time.**

## Sequence lock

**Background:** dusty lavender `#756B8F`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

PROCESS_BASE: tiny station-passenger pictogram cloned for all processes; USER_A: angular observer holding one photograph; PHOTO_A fixed rectangular print. All process clones share one base silhouette, state changes only by presence/pose.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
PROCESS_BASE: tiny station-passenger pictogram cloned for all processes; USER_A: angular observer holding one photograph; PHOTO_A fixed rectangular print. All process clones share one base silhouette, state changes only by presence/pose.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `24_ps_frame_00.png`

**Used as:** Cut 01 START

A busy station filled with tiny process characters moving in different directions. Suddenly everything freezes and a camera flash fires with the label `ps`.

### Frame 01 — `24_ps_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

User holding the fresh photo.

### Frame 02 — `24_ps_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Finger on photo.

### Frame 03 — `24_ps_frame_03.png`

**Used as:** Cut 03 END

A caption appears `CURRENT PROCESSES`; a tiny pencil enters and adds underneath `AT THE TIME.` Hold on photo versus reality.

## Cut 01 — 00:00–05:00

**START:** `24_ps_frame_00.png`

**END:** `24_ps_frame_01.png`

A busy station filled with tiny process characters moving in different directions. Suddenly everything freezes and a camera flash fires with the label `ps`. A photograph drops from a slot showing that exact frozen arrangement. End with user holding the fresh photo.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty lavender #756B8F
Character/prop lock: PROCESS_BASE: tiny station-passenger pictogram cloned for all processes; USER_A: angular observer holding one photograph; PHOTO_A fixed rectangular print. All process clones share one base silhouette, state changes only by presence/pose.
Reference board: use the canonical sequence reference board for Animation 24.
START FRAME: 24_ps_frame_00.png
END FRAME: 24_ps_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A busy station filled with tiny process characters moving in different directions. Suddenly everything freezes and a camera flash fires with the label `ps`. A photograph drops from a slot showing that exact frozen arrangement. End with user holding the fresh photo.
```

## Cut 02 — 05:00–10:00

**START:** `24_ps_frame_01.png`

**END:** `24_ps_frame_02.png`

The live station immediately resumes and changes fast: several processes leave, new ones arrive, one catches fire, another changes queues. The user studies the static photograph without looking up and points confidently at a process shown there. End with finger on photo.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty lavender #756B8F
Character/prop lock: PROCESS_BASE: tiny station-passenger pictogram cloned for all processes; USER_A: angular observer holding one photograph; PHOTO_A fixed rectangular print. All process clones share one base silhouette, state changes only by presence/pose.
Reference board: use the canonical sequence reference board for Animation 24.
START FRAME: 24_ps_frame_01.png
END FRAME: 24_ps_frame_02.png

CUT 02 OF 03 — 05:00–10:00
The live station immediately resumes and changes fast: several processes leave, new ones arrive, one catches fire, another changes queues. The user studies the static photograph without looking up and points confidently at a process shown there. End with finger on photo.
```

## Cut 03 — 10:00–15:00

**START:** `24_ps_frame_02.png`

**END:** `24_ps_frame_03.png`

Reveal that the pointed-to process is no longer present and the station behind the user is completely different. A caption appears `CURRENT PROCESSES`; a tiny pencil enters and adds underneath `AT THE TIME.` Hold on photo versus reality.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dusty lavender #756B8F
Character/prop lock: PROCESS_BASE: tiny station-passenger pictogram cloned for all processes; USER_A: angular observer holding one photograph; PHOTO_A fixed rectangular print. All process clones share one base silhouette, state changes only by presence/pose.
Reference board: use the canonical sequence reference board for Animation 24.
START FRAME: 24_ps_frame_02.png
END FRAME: 24_ps_frame_03.png

CUT 03 OF 03 — 10:00–15:00
Reveal that the pointed-to process is no longer present and the station behind the user is completely different. A caption appears `CURRENT PROCESSES`; a tiny pencil enters and adds underneath `AT THE TIME.` Hold on photo versus reality.
```


---

# Animation 25 — `grep` — pattern search

## chapter_09 — Looping the Loop

## Source animation sequence

A vast rubbish heap of text.

A tiny searchlight shaped like a regular expression sweeps across it.

It catches one word.

Instead of lifting the word out, a crane grabs the entire line.

The line is absurdly long.

It drags half the rubbish heap with it.

A small sign appears:

**MATCH FOUND**

Underneath:

**and quite a lot of its line.**

## Sequence lock

**Background:** charcoal navy `#1D2834`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

SEARCHLIGHT_A: fixed conical beam mounted to simple lamp head; CRANE_A: rigid geometric crane with one hook; TEXT_LINE_A: flat strip of glyph blocks. No living character redesign required.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
SEARCHLIGHT_A: fixed conical beam mounted to simple lamp head; CRANE_A: rigid geometric crane with one hook; TEXT_LINE_A: flat strip of glyph blocks. No living character redesign required.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `25_grep_frame_00.png`

**Used as:** Cut 01 START

A huge rubbish heap made from strips of text. A tiny searchlight whose beam is shaped like a regular expression scans across it and locks onto one highlighted word.

### Frame 01 — `25_grep_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Word glowing under beam.

### Frame 02 — `25_grep_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Crane straining under a ridiculous line.

### Frame 03 — `25_grep_frame_03.png`

**Used as:** Cut 03 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `25_grep_frame_00.png`

**END:** `25_grep_frame_01.png`

A huge rubbish heap made from strips of text. A tiny searchlight whose beam is shaped like a regular expression scans across it and locks onto one highlighted word. End with word glowing under beam.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: charcoal navy #1D2834
Character/prop lock: SEARCHLIGHT_A: fixed conical beam mounted to simple lamp head; CRANE_A: rigid geometric crane with one hook; TEXT_LINE_A: flat strip of glyph blocks. No living character redesign required.
Reference board: use the canonical sequence reference board for Animation 25.
START FRAME: 25_grep_frame_00.png
END FRAME: 25_grep_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A huge rubbish heap made from strips of text. A tiny searchlight whose beam is shaped like a regular expression scans across it and locks onto one highlighted word. End with word glowing under beam.
```

## Cut 02 — 05:00–10:00

**START:** `25_grep_frame_01.png`

**END:** `25_grep_frame_02.png`

Instead of plucking out the word, a crane descends and grabs the entire line containing it. The line is absurdly long and begins pulling half the text heap sideways with it, letters and scraps attached. End with crane straining under a ridiculous line.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: charcoal navy #1D2834
Character/prop lock: SEARCHLIGHT_A: fixed conical beam mounted to simple lamp head; CRANE_A: rigid geometric crane with one hook; TEXT_LINE_A: flat strip of glyph blocks. No living character redesign required.
Reference board: use the canonical sequence reference board for Animation 25.
START FRAME: 25_grep_frame_01.png
END FRAME: 25_grep_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Instead of plucking out the word, a crane descends and grabs the entire line containing it. The line is absurdly long and begins pulling half the text heap sideways with it, letters and scraps attached. End with crane straining under a ridiculous line.
```

## Cut 03 — 10:00–15:00

**START:** `25_grep_frame_02.png`

**END:** `25_grep_frame_03.png`

A neat sign pops up: `MATCH FOUND`. Beneath it a smaller bureaucratic amendment appears: `AND QUITE A LOT OF ITS LINE.` The crane continues dragging the line out of frame while the searchlight proudly stays on the original word. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: charcoal navy #1D2834
Character/prop lock: SEARCHLIGHT_A: fixed conical beam mounted to simple lamp head; CRANE_A: rigid geometric crane with one hook; TEXT_LINE_A: flat strip of glyph blocks. No living character redesign required.
Reference board: use the canonical sequence reference board for Animation 25.
START FRAME: 25_grep_frame_02.png
END FRAME: 25_grep_frame_03.png

CUT 03 OF 03 — 10:00–15:00
A neat sign pops up: `MATCH FOUND`. Beneath it a smaller bureaucratic amendment appears: `AND QUITE A LOT OF ITS LINE.` The crane continues dragging the line out of frame while the searchlight proudly stays on the original word. Hold.
```


---

# Animation 26 — `ifconfig` — interface configuration

## chapter_10 — The Ultimate Handshake

## Source animation sequence

A nervous interface sits at a desk wearing a mask.

An investigator asks:

**"Address?"**

It hands over the address.

**"Mask?"**

It hands over the mask.

The investigator combines them and marks the network prefix.

The interface looks suddenly exposed.

A second investigator from Android Security enters and closes the file cabinet.

`Permission denied`

Everyone agrees the mask was not the main problem.

## Sequence lock

**Background:** pale pink `#BD8E98`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

INTERFACE_A: nervous rectangular network-interface character with one removable mask prop; INVESTIGATOR_A: thin official with overlay cards; ANDROID_SECURITY_A: tall rigid guard with stamp. Preserve exact mask and face geometry.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
INTERFACE_A: nervous rectangular network-interface character with one removable mask prop; INVESTIGATOR_A: thin official with overlay cards; ANDROID_SECURITY_A: tall rigid guard with stamp. Preserve exact mask and face geometry.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `26_ifconfig_frame_00.png`

**Used as:** Cut 01 START

A nervous network interface sits at a desk wearing a literal subnet mask. An investigator asks `ADDRESS?`; interface hands over an address card.

### Frame 01 — `26_ifconfig_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Investigator holding both cards.

### Frame 02 — `26_ifconfig_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Clean prefix diagram.

### Frame 03 — `26_ifconfig_frame_03.png`

**Used as:** Cut 03 END

The security restriction becoming the real problem.

## Cut 01 — 00:00–05:00

**START:** `26_ifconfig_frame_00.png`

**END:** `26_ifconfig_frame_01.png`

A nervous network interface sits at a desk wearing a literal subnet mask. An investigator asks `ADDRESS?`; interface hands over an address card. `MASK?`; interface removes the mask and hands it over too. End with investigator holding both cards.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale pink #BD8E98
Character/prop lock: INTERFACE_A: nervous rectangular network-interface character with one removable mask prop; INVESTIGATOR_A: thin official with overlay cards; ANDROID_SECURITY_A: tall rigid guard with stamp. Preserve exact mask and face geometry.
Reference board: use the canonical sequence reference board for Animation 26.
START FRAME: 26_ifconfig_frame_00.png
END FRAME: 26_ifconfig_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A nervous network interface sits at a desk wearing a literal subnet mask. An investigator asks `ADDRESS?`; interface hands over an address card. `MASK?`; interface removes the mask and hands it over too. End with investigator holding both cards.
```

## Cut 02 — 05:00–10:00

**START:** `26_ifconfig_frame_01.png`

**END:** `26_ifconfig_frame_02.png`

The investigator overlays address and mask like transparent stencils and marks the resulting network prefix on a chart. The interface, now unmasked, looks visually exposed by simply sitting there without its face-covering shape. End on clean prefix diagram.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale pink #BD8E98
Character/prop lock: INTERFACE_A: nervous rectangular network-interface character with one removable mask prop; INVESTIGATOR_A: thin official with overlay cards; ANDROID_SECURITY_A: tall rigid guard with stamp. Preserve exact mask and face geometry.
Reference board: use the canonical sequence reference board for Animation 26.
START FRAME: 26_ifconfig_frame_01.png
END FRAME: 26_ifconfig_frame_02.png

CUT 02 OF 03 — 05:00–10:00
The investigator overlays address and mask like transparent stencils and marks the resulting network prefix on a chart. The interface, now unmasked, looks visually exposed by simply sitting there without its face-covering shape. End on clean prefix diagram.
```

## Cut 03 — 10:00–15:00

**START:** `26_ifconfig_frame_02.png`

**END:** `26_ifconfig_frame_03.png`

A second investigator labeled `ANDROID SECURITY` enters, shuts the filing cabinet containing further interface data, and stamps `PERMISSION DENIED`. Everyone looks at the discarded mask, then the locked cabinet. Hold on the security restriction becoming the real problem.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale pink #BD8E98
Character/prop lock: INTERFACE_A: nervous rectangular network-interface character with one removable mask prop; INVESTIGATOR_A: thin official with overlay cards; ANDROID_SECURITY_A: tall rigid guard with stamp. Preserve exact mask and face geometry.
Reference board: use the canonical sequence reference board for Animation 26.
START FRAME: 26_ifconfig_frame_02.png
END FRAME: 26_ifconfig_frame_03.png

CUT 03 OF 03 — 10:00–15:00
A second investigator labeled `ANDROID SECURITY` enters, shuts the filing cabinet containing further interface data, and stamps `PERMISSION DENIED`. Everyone looks at the discarded mask, then the locked cabinet. Hold on the security restriction becoming the real problem.
```


---

# Animation 27 — `man` — manual

## Chapter 16 — Documentation

## Source animation sequence

A user opens:

`man command`

At the bottom:

`SEE ALSO: other(1)`

They follow it.

Another page points to three more.

Pages branch into a paper maze.

At the centre, the original command quietly performs the desired operation.

The user is elsewhere reading about magnetic tape.

A sign appears:

**YOU ARE HERE**

It points to the manual.

## Sequence lock

**Background:** warm cream `#C9BE9E`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

USER_A: angular human pictogram; MANUAL_PAGE_BASE: identical paper panel cloned for every manual page; COMMAND_A: tiny fixed machine/command object at maze center. Page layout may multiply; user does not redesign.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
USER_A: angular human pictogram; MANUAL_PAGE_BASE: identical paper panel cloned for every manual page; COMMAND_A: tiny fixed machine/command object at maze center. Page layout may multiply; user does not redesign.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `27_man_frame_00.png`

**Used as:** Cut 01 START

A user opens `man command`. The manual page is a clean paper panel.

### Frame 01 — `27_man_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Three choices branching.

### Frame 02 — `27_man_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

User walking away from center.

### Frame 03 — `27_man_frame_03.png`

**Used as:** Cut 03 END

Technically accurate navigation.

## Cut 01 — 00:00–05:00

**START:** `27_man_frame_00.png`

**END:** `27_man_frame_01.png`

A user opens `man command`. The manual page is a clean paper panel. At the bottom, `SEE ALSO: other(1)` is highlighted. The user follows it; the panel slides sideways to another page with three more `SEE ALSO` arrows. End with three choices branching.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: warm cream #C9BE9E
Character/prop lock: USER_A: angular human pictogram; MANUAL_PAGE_BASE: identical paper panel cloned for every manual page; COMMAND_A: tiny fixed machine/command object at maze center. Page layout may multiply; user does not redesign.
Reference board: use the canonical sequence reference board for Animation 27.
START FRAME: 27_man_frame_00.png
END FRAME: 27_man_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A user opens `man command`. The manual page is a clean paper panel. At the bottom, `SEE ALSO: other(1)` is highlighted. The user follows it; the panel slides sideways to another page with three more `SEE ALSO` arrows. End with three choices branching.
```

## Cut 02 — 05:00–10:00

**START:** `27_man_frame_01.png`

**END:** `27_man_frame_02.png`

The pages rapidly branch into a literal paper maze around the user, each corridor made of manual pages and cross-references. Keep typography mostly abstract except section labels. In a small visible window at the maze center, the original command quietly performs the desired operation by itself. End with user walking away from center.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: warm cream #C9BE9E
Character/prop lock: USER_A: angular human pictogram; MANUAL_PAGE_BASE: identical paper panel cloned for every manual page; COMMAND_A: tiny fixed machine/command object at maze center. Page layout may multiply; user does not redesign.
Reference board: use the canonical sequence reference board for Animation 27.
START FRAME: 27_man_frame_01.png
END FRAME: 27_man_frame_02.png

CUT 02 OF 03 — 05:00–10:00
The pages rapidly branch into a literal paper maze around the user, each corridor made of manual pages and cross-references. Keep typography mostly abstract except section labels. In a small visible window at the maze center, the original command quietly performs the desired operation by itself. End with user walking away from center.
```

## Cut 03 — 10:00–15:00

**START:** `27_man_frame_02.png`

**END:** `27_man_frame_03.png`

The user has wandered into a remote branch labeled `MAGNETIC TAPE`. A large `YOU ARE HERE` sign appears. Its arrow points not to the desired command, but to the current manual page. Hold on technically accurate navigation.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: warm cream #C9BE9E
Character/prop lock: USER_A: angular human pictogram; MANUAL_PAGE_BASE: identical paper panel cloned for every manual page; COMMAND_A: tiny fixed machine/command object at maze center. Page layout may multiply; user does not redesign.
Reference board: use the canonical sequence reference board for Animation 27.
START FRAME: 27_man_frame_02.png
END FRAME: 27_man_frame_03.png

CUT 03 OF 03 — 10:00–15:00
The user has wandered into a remote branch labeled `MAGNETIC TAPE`. A large `YOU ARE HERE` sign appears. Its arrow points not to the desired command, but to the current manual page. Hold on technically accurate navigation.
```


---

# Animation 28 — `history` — shell history

## Chapter 17 — Time & Memory

## Source animation sequence

A historian opens an enormous volume labelled:

**SHELL HISTORY**

Lines of commands stretch backward for years.

The historian stops at:

`rm important_file`

Long silence.

They look for the preceding question.

There isn't one.

A marginal note is added:

**MOTIVE UNKNOWN**

## Sequence lock

**Background:** sepia cream `#B6A27C`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

HISTORIAN_A: thin figure with round spectacles and one pointer finger; HISTORY_BOOK_A: enormous fixed bound volume; all pages inherit the same book geometry. Preserve historian and book exactly.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
HISTORIAN_A: thin figure with round spectacles and one pointer finger; HISTORY_BOOK_A: enormous fixed bound volume; all pages inherit the same book geometry. Preserve historian and book exactly.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `28_history_frame_00.png`

**Used as:** Cut 01 START

A historian opens an enormous bound volume labeled `SHELL HISTORY`. Command lines extend backward through layered pages for years.

### Frame 01 — `28_history_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Finger frozen on that line.

### Frame 02 — `28_history_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Empty margin beside it.

### Frame 03 — `28_history_frame_03.png`

**Used as:** Cut 03 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `28_history_frame_00.png`

**END:** `28_history_frame_01.png`

A historian opens an enormous bound volume labeled `SHELL HISTORY`. Command lines extend backward through layered pages for years. The historian traces a finger down entries until stopping at `rm important_file`. End with finger frozen on that line.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sepia cream #B6A27C
Character/prop lock: HISTORIAN_A: thin figure with round spectacles and one pointer finger; HISTORY_BOOK_A: enormous fixed bound volume; all pages inherit the same book geometry. Preserve historian and book exactly.
Reference board: use the canonical sequence reference board for Animation 28.
START FRAME: 28_history_frame_00.png
END FRAME: 28_history_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A historian opens an enormous bound volume labeled `SHELL HISTORY`. Command lines extend backward through layered pages for years. The historian traces a finger down entries until stopping at `rm important_file`. End with finger frozen on that line.
```

## Cut 02 — 05:00–10:00

**START:** `28_history_frame_01.png`

**END:** `28_history_frame_02.png`

Long visual pause. The historian flips backward one page, then another, looking for an earlier question, explanation, or motive. Only unrelated commands appear. The historian flips forward again to `rm important_file`. End with empty margin beside it.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sepia cream #B6A27C
Character/prop lock: HISTORIAN_A: thin figure with round spectacles and one pointer finger; HISTORY_BOOK_A: enormous fixed bound volume; all pages inherit the same book geometry. Preserve historian and book exactly.
Reference board: use the canonical sequence reference board for Animation 28.
START FRAME: 28_history_frame_01.png
END FRAME: 28_history_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Long visual pause. The historian flips backward one page, then another, looking for an earlier question, explanation, or motive. Only unrelated commands appear. The historian flips forward again to `rm important_file`. End with empty margin beside it.
```

## Cut 03 — 10:00–15:00

**START:** `28_history_frame_02.png`

**END:** `28_history_frame_03.png`

The historian writes a sober marginal annotation next to the command: `MOTIVE UNKNOWN`. The book closes a few centimeters, then stops as if history itself refuses closure. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sepia cream #B6A27C
Character/prop lock: HISTORIAN_A: thin figure with round spectacles and one pointer finger; HISTORY_BOOK_A: enormous fixed bound volume; all pages inherit the same book geometry. Preserve historian and book exactly.
Reference board: use the canonical sequence reference board for Animation 28.
START FRAME: 28_history_frame_02.png
END FRAME: 28_history_frame_03.png

CUT 03 OF 03 — 10:00–15:00
The historian writes a sober marginal annotation next to the command: `MOTIVE UNKNOWN`. The book closes a few centimeters, then stops as if history itself refuses closure. Hold.
```


---

# Animation 29 — `touch` — touch

## Chapter 17 — Time & Memory

## Source animation sequence

A finger approaches an empty patch of screen.

It never quite makes contact.

`touch newfile`

A file appears anyway.

The finger looks offended.

It points at an old file.

`touch oldfile`

A clock above the file jumps forward.

The file itself does nothing.

A theologian enters, opens a notebook, and writes:

**PROMISING.**

## Sequence lock

**Background:** pale cyan `#8DB8B7`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

FINGER_A: oversized simplified hand with square fingertip; FILE_NEW_A and FILE_OLD_A: identical file-card geometry; THEOLOGIAN_A: tall narrow figure with small notebook. Keep exact silhouettes across all cuts.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
FINGER_A: oversized simplified hand with square fingertip; FILE_NEW_A and FILE_OLD_A: identical file-card geometry; THEOLOGIAN_A: tall narrow figure with small notebook. Keep exact silhouettes across all cuts.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `29_touch_frame_00.png`

**Used as:** Cut 01 START

A giant simplified finger approaches an empty patch of the scene but stops a tiny distance short of touching it.

### Frame 01 — `29_touch_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

New file.

### Frame 02 — `29_touch_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Finger comparing old-looking file to new timestamp.

### Frame 03 — `29_touch_frame_03.png`

**Used as:** Cut 03 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `29_touch_frame_00.png`

**END:** `29_touch_frame_01.png`

A giant simplified finger approaches an empty patch of the scene but stops a tiny distance short of touching it. Command label `touch newfile`. Despite no physical contact, a new file card pops into existence. The finger recoils slightly, offended by irrelevance. End on new file.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale cyan #8DB8B7
Character/prop lock: FINGER_A: oversized simplified hand with square fingertip; FILE_NEW_A and FILE_OLD_A: identical file-card geometry; THEOLOGIAN_A: tall narrow figure with small notebook. Keep exact silhouettes across all cuts.
Reference board: use the canonical sequence reference board for Animation 29.
START FRAME: 29_touch_frame_00.png
END FRAME: 29_touch_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A giant simplified finger approaches an empty patch of the scene but stops a tiny distance short of touching it. Command label `touch newfile`. Despite no physical contact, a new file card pops into existence. The finger recoils slightly, offended by irrelevance. End on new file.
```

## Cut 02 — 05:00–10:00

**START:** `29_touch_frame_01.png`

**END:** `29_touch_frame_02.png`

The finger points toward an old existing file. `touch oldfile`. A clock icon above the file jumps forward to a newer time while the file itself remains perfectly motionless and unchanged. End with finger comparing old-looking file to new timestamp.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale cyan #8DB8B7
Character/prop lock: FINGER_A: oversized simplified hand with square fingertip; FILE_NEW_A and FILE_OLD_A: identical file-card geometry; THEOLOGIAN_A: tall narrow figure with small notebook. Keep exact silhouettes across all cuts.
Reference board: use the canonical sequence reference board for Animation 29.
START FRAME: 29_touch_frame_01.png
END FRAME: 29_touch_frame_02.png

CUT 02 OF 03 — 05:00–10:00
The finger points toward an old existing file. `touch oldfile`. A clock icon above the file jumps forward to a newer time while the file itself remains perfectly motionless and unchanged. End with finger comparing old-looking file to new timestamp.
```

## Cut 03 — 10:00–15:00

**START:** `29_touch_frame_02.png`

**END:** `29_touch_frame_03.png`

A theologian enters carrying a notebook, observes the finger that never touched anything and the altered time, then writes one word in large caps: `PROMISING.` The finger points accusingly at the theologian. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale cyan #8DB8B7
Character/prop lock: FINGER_A: oversized simplified hand with square fingertip; FILE_NEW_A and FILE_OLD_A: identical file-card geometry; THEOLOGIAN_A: tall narrow figure with small notebook. Keep exact silhouettes across all cuts.
Reference board: use the canonical sequence reference board for Animation 29.
START FRAME: 29_touch_frame_02.png
END FRAME: 29_touch_frame_03.png

CUT 03 OF 03 — 10:00–15:00
A theologian enters carrying a notebook, observes the finger that never touched anything and the altered time, then writes one word in large caps: `PROMISING.` The finger points accusingly at the theologian. Hold.
```


---

# Animation 30 — `yes` — yes

## Chapter 18 — Agreement

## Source animation sequence

A small machine is asked:

**"Continue?"**

`YES`

**"Again?"**

`YES`

The questioner leaves.

`YES`

`YES`

Centuries pass.

Ruins.

`YES`

An archaeologist arrives.

The machine says:

`YES`

The archaeologist has not yet asked anything.

Artificial Wisdom quietly labels the exhibit:

**PRE-INQUIRY PERIOD**

## Sequence lock

**Background:** dark teal `#174A4A`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

YES_MACHINE_A: squat little box with one output slot and two tiny feet; QUESTIONER_A: thin standing figure; ARCHAEOLOGIST_A: bent figure with small satchel. YES_MACHINE_A must remain absolutely unchanged across centuries.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
YES_MACHINE_A: squat little box with one output slot and two tiny feet; QUESTIONER_A: thin standing figure; ARCHAEOLOGIST_A: bent figure with small satchel. YES_MACHINE_A must remain absolutely unchanged across centuries.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `30_yes_frame_00.png`

**Used as:** Cut 01 START

A tiny machine sits across from a questioner. Card: `CONTINUE?` Machine instantly outputs `YES`.

### Frame 01 — `30_yes_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Machine alone.

### Frame 02 — `30_yes_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

End centuries later on ruined room and another `YES`.

### Frame 03 — `30_yes_frame_03.png`

**Used as:** Cut 03 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `30_yes_frame_00.png`

**END:** `30_yes_frame_01.png`

A tiny machine sits across from a questioner. Card: `CONTINUE?` Machine instantly outputs `YES`. Card: `AGAIN?` Machine outputs `YES`. The questioner nods, then walks out of frame. End with machine alone.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark teal #174A4A
Character/prop lock: YES_MACHINE_A: squat little box with one output slot and two tiny feet; QUESTIONER_A: thin standing figure; ARCHAEOLOGIST_A: bent figure with small satchel. YES_MACHINE_A must remain absolutely unchanged across centuries.
Reference board: use the canonical sequence reference board for Animation 30.
START FRAME: 30_yes_frame_00.png
END FRAME: 30_yes_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A tiny machine sits across from a questioner. Card: `CONTINUE?` Machine instantly outputs `YES`. Card: `AGAIN?` Machine outputs `YES`. The questioner nods, then walks out of frame. End with machine alone.
```

## Cut 02 — 05:00–10:00

**START:** `30_yes_frame_01.png`

**END:** `30_yes_frame_02.png`

With nobody present, the machine continues emitting `YES` at a steady mechanical rhythm. The room ages around it through rapid graphic wipes: clean room, dust, cracks, ruins. The machine never changes cadence. End centuries later on ruined room and another `YES`.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark teal #174A4A
Character/prop lock: YES_MACHINE_A: squat little box with one output slot and two tiny feet; QUESTIONER_A: thin standing figure; ARCHAEOLOGIST_A: bent figure with small satchel. YES_MACHINE_A must remain absolutely unchanged across centuries.
Reference board: use the canonical sequence reference board for Animation 30.
START FRAME: 30_yes_frame_01.png
END FRAME: 30_yes_frame_02.png

CUT 02 OF 03 — 05:00–10:00
With nobody present, the machine continues emitting `YES` at a steady mechanical rhythm. The room ages around it through rapid graphic wipes: clean room, dust, cracks, ruins. The machine never changes cadence. End centuries later on ruined room and another `YES`.
```

## Cut 03 — 10:00–15:00

**START:** `30_yes_frame_02.png`

**END:** `30_yes_frame_03.png`

An archaeologist enters the ruins. Before asking anything, the machine says `YES`. The archaeologist freezes. A quiet Artificial Wisdom museum label slides beneath the machine: `PRE-INQUIRY PERIOD`. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark teal #174A4A
Character/prop lock: YES_MACHINE_A: squat little box with one output slot and two tiny feet; QUESTIONER_A: thin standing figure; ARCHAEOLOGIST_A: bent figure with small satchel. YES_MACHINE_A must remain absolutely unchanged across centuries.
Reference board: use the canonical sequence reference board for Animation 30.
START FRAME: 30_yes_frame_02.png
END FRAME: 30_yes_frame_03.png

CUT 03 OF 03 — 10:00–15:00
An archaeologist enters the ruins. Before asking anything, the machine says `YES`. The archaeologist freezes. A quiet Artificial Wisdom museum label slides beneath the machine: `PRE-INQUIRY PERIOD`. Hold.
```


---

# Animation 31 — `false` — false

## Chapter 18 — Agreement

## Source animation sequence

Two identical boxes sit side by side.

Press `true`.

Nothing visible happens.

`status: 0`

Press `false`.

Nothing visible happens.

`status: 1`

A researcher asks:

**"What proposition did you evaluate?"**

Both boxes remain silent.

For once, this is the technically correct answer.

## Sequence lock

**Background:** pale blue `#7899AF`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

TRUE_BOX_A and FALSE_BOX_A: exact identical box geometry; RESEARCHER_A: narrow figure with comparison card. The boxes must remain visually identical except status strip text/value.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
TRUE_BOX_A and FALSE_BOX_A: exact identical box geometry; RESEARCHER_A: narrow figure with comparison card. The boxes must remain visually identical except status strip text/value.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `31_false_frame_00.png`

**Used as:** Cut 01 START

Two identical simple boxes sit side by side. A hand presses `true`.

### Frame 01 — `31_false_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Visually identical boxes with different status strips.

### Frame 02 — `31_false_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Prolonged silence.

### Frame 03 — `31_false_frame_03.png`

**Used as:** Cut 03 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `31_false_frame_00.png`

**END:** `31_false_frame_01.png`

Two identical simple boxes sit side by side. A hand presses `true`. Nothing visible happens; a tiny status strip appears `status: 0`. The hand presses `false`. Again nothing visible happens; strip reads `status: 1`. End on visually identical boxes with different status strips.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale blue #7899AF
Character/prop lock: TRUE_BOX_A and FALSE_BOX_A: exact identical box geometry; RESEARCHER_A: narrow figure with comparison card. The boxes must remain visually identical except status strip text/value.
Reference board: use the canonical sequence reference board for Animation 31.
START FRAME: 31_false_frame_00.png
END FRAME: 31_false_frame_01.png

CUT 01 OF 03 — 00:00–05:00
Two identical simple boxes sit side by side. A hand presses `true`. Nothing visible happens; a tiny status strip appears `status: 0`. The hand presses `false`. Again nothing visible happens; strip reads `status: 1`. End on visually identical boxes with different status strips.
```

## Cut 02 — 05:00–10:00

**START:** `31_false_frame_01.png`

**END:** `31_false_frame_02.png`

A researcher enters and compares the boxes, taps them, looks behind them, finds no visible behavioral difference. They raise a card: `WHAT PROPOSITION DID YOU EVALUATE?` The boxes do absolutely nothing. End on prolonged silence.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale blue #7899AF
Character/prop lock: TRUE_BOX_A and FALSE_BOX_A: exact identical box geometry; RESEARCHER_A: narrow figure with comparison card. The boxes must remain visually identical except status strip text/value.
Reference board: use the canonical sequence reference board for Animation 31.
START FRAME: 31_false_frame_01.png
END FRAME: 31_false_frame_02.png

CUT 02 OF 03 — 05:00–10:00
A researcher enters and compares the boxes, taps them, looks behind them, finds no visible behavioral difference. They raise a card: `WHAT PROPOSITION DID YOU EVALUATE?` The boxes do absolutely nothing. End on prolonged silence.
```

## Cut 03 — 10:00–15:00

**START:** `31_false_frame_02.png`

**END:** `31_false_frame_03.png`

The researcher waits. A small editorial annotation appears beside the silence: `TECHNICALLY CORRECT`. The two boxes remain identical and inactive while their status strips continue to disagree. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale blue #7899AF
Character/prop lock: TRUE_BOX_A and FALSE_BOX_A: exact identical box geometry; RESEARCHER_A: narrow figure with comparison card. The boxes must remain visually identical except status strip text/value.
Reference board: use the canonical sequence reference board for Animation 31.
START FRAME: 31_false_frame_02.png
END FRAME: 31_false_frame_03.png

CUT 03 OF 03 — 10:00–15:00
The researcher waits. A small editorial annotation appears beside the silence: `TECHNICALLY CORRECT`. The two boxes remain identical and inactive while their status strips continue to disagree. Hold.
```


---

# Animation 32 — `|` — pipe

## Chapter 19 — Pipes & Redirection

## Source animation sequence

A command speaks into a large brass pipe.

Words shoot through.

At the far end another command receives them, crosses several out, circles two, rearranges the rest and sends them through another pipe.

Soon an entire city of commands is connected by pipes carrying text in every direction.

A researcher asks:

**"Who said that?"**

Every pipe points left.

The camera follows the pointing pipes until they disappear off-screen.

## Sequence lock

**Background:** dark navy `#142338`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

COMMAND_BASE: identical little command-person clone; RESEARCHER_A: one narrow foreground figure holding a statement; PIPE_NETWORK_A: fixed-width pipes with consistent arrowheads. Reuse command clone silhouette across whole city.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
COMMAND_BASE: identical little command-person clone; RESEARCHER_A: one narrow foreground figure holding a statement; PIPE_NETWORK_A: fixed-width pipes with consistent arrowheads. Reuse command clone silhouette across whole city.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `32_pipe_frame_00.png`

**Used as:** Cut 01 START

A command character speaks words into a large brass-colored pipe. The words travel visibly through the pipe to a second command, which receives them and immediately crosses some out, circles two, rearranges the rest.

### Frame 01 — `32_pipe_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Second command feeding modified words into another pipe.

### Frame 02 — `32_pipe_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Researcher looking around for source.

### Frame 03 — `32_pipe_frame_03.png`

**Used as:** Cut 03 END

End mid-follow on endless leftward arrows.

## Cut 01 — 00:00–05:00

**START:** `32_pipe_frame_00.png`

**END:** `32_pipe_frame_01.png`

A command character speaks words into a large brass-colored pipe. The words travel visibly through the pipe to a second command, which receives them and immediately crosses some out, circles two, rearranges the rest. End with second command feeding modified words into another pipe.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #142338
Character/prop lock: COMMAND_BASE: identical little command-person clone; RESEARCHER_A: one narrow foreground figure holding a statement; PIPE_NETWORK_A: fixed-width pipes with consistent arrowheads. Reuse command clone silhouette across whole city.
Reference board: use the canonical sequence reference board for Animation 32.
START FRAME: 32_pipe_frame_00.png
END FRAME: 32_pipe_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A command character speaks words into a large brass-colored pipe. The words travel visibly through the pipe to a second command, which receives them and immediately crosses some out, circles two, rearranges the rest. End with second command feeding modified words into another pipe.
```

## Cut 02 — 05:00–10:00

**START:** `32_pipe_frame_01.png`

**END:** `32_pipe_frame_02.png`

Pull back to reveal an entire flat city of commands connected by pipes carrying text in every direction. Some pipes branch, some converge, but flow direction is always clear. A researcher enters the foreground holding one received statement. End with researcher looking around for source.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #142338
Character/prop lock: COMMAND_BASE: identical little command-person clone; RESEARCHER_A: one narrow foreground figure holding a statement; PIPE_NETWORK_A: fixed-width pipes with consistent arrowheads. Reuse command clone silhouette across whole city.
Reference board: use the canonical sequence reference board for Animation 32.
START FRAME: 32_pipe_frame_01.png
END FRAME: 32_pipe_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Pull back to reveal an entire flat city of commands connected by pipes carrying text in every direction. Some pipes branch, some converge, but flow direction is always clear. A researcher enters the foreground holding one received statement. End with researcher looking around for source.
```

## Cut 03 — 10:00–15:00

**START:** `32_pipe_frame_02.png`

**END:** `32_pipe_frame_03.png`

Researcher asks `WHO SAID THAT?` Every pipe in the city simultaneously grows a tiny arrow pointing left. Camera begins a flat lateral follow across pipes, then another, then another until the network disappears offscreen with no original speaker found. End mid-follow on endless leftward arrows.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: dark navy #142338
Character/prop lock: COMMAND_BASE: identical little command-person clone; RESEARCHER_A: one narrow foreground figure holding a statement; PIPE_NETWORK_A: fixed-width pipes with consistent arrowheads. Reuse command clone silhouette across whole city.
Reference board: use the canonical sequence reference board for Animation 32.
START FRAME: 32_pipe_frame_02.png
END FRAME: 32_pipe_frame_03.png

CUT 03 OF 03 — 10:00–15:00
Researcher asks `WHO SAID THAT?` Every pipe in the city simultaneously grows a tiny arrow pointing left. Camera begins a flat lateral follow across pipes, then another, then another until the network disappears offscreen with no original speaker found. End mid-follow on endless leftward arrows.
```


---

# Animation 33 — `cp` — copy

## Chapter 20 — Copies & Movement

## Source animation sequence

A file enters a copying booth.

Two files emerge with identical contents.

An inspector compares their name cards.

Different.

Checks timestamps and permissions.

Possibly different.

Checks the data.

Same.

The inspector writes:

**COPY**

then adds, in smaller letters:

**subject to options and local law**

## Sequence lock

**Background:** pale pink `#B98590`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

FILE_ORIGINAL_A and FILE_COPY_A: exact identical file-card geometry after copy; INSPECTOR_A: thin official with report stamp; COPY_BOOTH_A fixed box machine. Files may differ only in labels/metadata marks specified by cut.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
FILE_ORIGINAL_A and FILE_COPY_A: exact identical file-card geometry after copy; INSPECTOR_A: thin official with report stamp; COPY_BOOTH_A fixed box machine. Files may differ only in labels/metadata marks specified by cut.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `33_cp_frame_00.png`

**Used as:** Cut 01 START

A file enters a simple copying booth. A light flashes once.

### Frame 01 — `33_cp_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Inspector holding both name cards.

### Frame 02 — `33_cp_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

The two identical data interiors aligned.

### Frame 03 — `33_cp_frame_03.png`

**Used as:** Cut 03 END

Report.

## Cut 01 — 00:00–05:00

**START:** `33_cp_frame_00.png`

**END:** `33_cp_frame_01.png`

A file enters a simple copying booth. A light flashes once. Two file cards emerge side by side with clearly identical internal data pattern but distinct name labels. An inspector steps between them. End with inspector holding both name cards.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale pink #B98590
Character/prop lock: FILE_ORIGINAL_A and FILE_COPY_A: exact identical file-card geometry after copy; INSPECTOR_A: thin official with report stamp; COPY_BOOTH_A fixed box machine. Files may differ only in labels/metadata marks specified by cut.
Reference board: use the canonical sequence reference board for Animation 33.
START FRAME: 33_cp_frame_00.png
END FRAME: 33_cp_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A file enters a simple copying booth. A light flashes once. Two file cards emerge side by side with clearly identical internal data pattern but distinct name labels. An inspector steps between them. End with inspector holding both name cards.
```

## Cut 02 — 05:00–10:00

**START:** `33_cp_frame_01.png`

**END:** `33_cp_frame_02.png`

Inspector compares names: different. Compares timestamp and permission stamps: possibly different. Then opens a small inspection window on both files and sees the exact same data pattern. End with the two identical data interiors aligned.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale pink #B98590
Character/prop lock: FILE_ORIGINAL_A and FILE_COPY_A: exact identical file-card geometry after copy; INSPECTOR_A: thin official with report stamp; COPY_BOOTH_A fixed box machine. Files may differ only in labels/metadata marks specified by cut.
Reference board: use the canonical sequence reference board for Animation 33.
START FRAME: 33_cp_frame_01.png
END FRAME: 33_cp_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Inspector compares names: different. Compares timestamp and permission stamps: possibly different. Then opens a small inspection window on both files and sees the exact same data pattern. End with the two identical data interiors aligned.
```

## Cut 03 — 10:00–15:00

**START:** `33_cp_frame_02.png`

**END:** `33_cp_frame_03.png`

Inspector stamps a large report `COPY`, pauses, then adds beneath in much smaller lettering `SUBJECT TO OPTIONS AND LOCAL LAW`. The two copied files roll away in different directions, equally legitimate. Hold on report.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: pale pink #B98590
Character/prop lock: FILE_ORIGINAL_A and FILE_COPY_A: exact identical file-card geometry after copy; INSPECTOR_A: thin official with report stamp; COPY_BOOTH_A fixed box machine. Files may differ only in labels/metadata marks specified by cut.
Reference board: use the canonical sequence reference board for Animation 33.
START FRAME: 33_cp_frame_02.png
END FRAME: 33_cp_frame_03.png

CUT 03 OF 03 — 10:00–15:00
Inspector stamps a large report `COPY`, pauses, then adds beneath in much smaller lettering `SUBJECT TO OPTIONS AND LOCAL LAW`. The two copied files roll away in different directions, equally legitimate. Hold on report.
```


---

# Animation 34 — `mv` — move

## Chapter 20 — Copies & Movement

## Source animation sequence

A file stands on a stage marked `/old/place/file.txt`.

`mv`

The scenery slides sideways.

The file remains perfectly still.

New sign:

`/new/place/file.txt`

A physicist applauds.

Then:

`mv file.txt answer.txt`

Only the name card changes.

The physicist stops applauding and begins filling out a form.

## Sequence lock

**Background:** sage `#73866D`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

FILE_A: fixed upright file-card object with one folded corner; PHYSICIST_A: tall thin figure with clipboard/form; STAGE_A fixed geometric floor/scenery system. The file never changes physical shape, only location/name labels.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
FILE_A: fixed upright file-card object with one folded corner; PHYSICIST_A: tall thin figure with clipboard/form; STAGE_A fixed geometric floor/scenery system. The file never changes physical shape, only location/name labels.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `34_mv_frame_00.png`

**Used as:** Cut 01 START

A file stands perfectly still on a stage whose floor label reads `/old/place/file.txt`. Command `mv`.

### Frame 01 — `34_mv_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

Stationary file, changed location label.

### Frame 02 — `34_mv_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Physicist staring at the renamed file.

### Frame 03 — `34_mv_frame_03.png`

**Used as:** Cut 03 END

Hold.

## Cut 01 — 00:00–05:00

**START:** `34_mv_frame_00.png`

**END:** `34_mv_frame_01.png`

A file stands perfectly still on a stage whose floor label reads `/old/place/file.txt`. Command `mv`. Instead of the file moving, the entire stage scenery slides sideways around it until the floor label becomes `/new/place/file.txt`. A physicist applauds. End on stationary file, changed location label.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sage #73866D
Character/prop lock: FILE_A: fixed upright file-card object with one folded corner; PHYSICIST_A: tall thin figure with clipboard/form; STAGE_A fixed geometric floor/scenery system. The file never changes physical shape, only location/name labels.
Reference board: use the canonical sequence reference board for Animation 34.
START FRAME: 34_mv_frame_00.png
END FRAME: 34_mv_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A file stands perfectly still on a stage whose floor label reads `/old/place/file.txt`. Command `mv`. Instead of the file moving, the entire stage scenery slides sideways around it until the floor label becomes `/new/place/file.txt`. A physicist applauds. End on stationary file, changed location label.
```

## Cut 02 — 05:00–10:00

**START:** `34_mv_frame_01.png`

**END:** `34_mv_frame_02.png`

Then command `mv file.txt answer.txt`. This time nothing in the scene moves except the file's name card, which flips from `file.txt` to `answer.txt`. The physicist's applause slows and stops. End with physicist staring at the renamed file.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sage #73866D
Character/prop lock: FILE_A: fixed upright file-card object with one folded corner; PHYSICIST_A: tall thin figure with clipboard/form; STAGE_A fixed geometric floor/scenery system. The file never changes physical shape, only location/name labels.
Reference board: use the canonical sequence reference board for Animation 34.
START FRAME: 34_mv_frame_01.png
END FRAME: 34_mv_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Then command `mv file.txt answer.txt`. This time nothing in the scene moves except the file's name card, which flips from `file.txt` to `answer.txt`. The physicist's applause slows and stops. End with physicist staring at the renamed file.
```

## Cut 03 — 10:00–15:00

**START:** `34_mv_frame_02.png`

**END:** `34_mv_frame_03.png`

Physicist pulls out a large bureaucratic form labeled `FRAME OF REFERENCE`, sits down, and begins filling it out while the file remains serenely still. A tiny `MOVED` stamp appears beside the unchanged object. Hold.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: sage #73866D
Character/prop lock: FILE_A: fixed upright file-card object with one folded corner; PHYSICIST_A: tall thin figure with clipboard/form; STAGE_A fixed geometric floor/scenery system. The file never changes physical shape, only location/name labels.
Reference board: use the canonical sequence reference board for Animation 34.
START FRAME: 34_mv_frame_02.png
END FRAME: 34_mv_frame_03.png

CUT 03 OF 03 — 10:00–15:00
Physicist pulls out a large bureaucratic form labeled `FRAME OF REFERENCE`, sits down, and begins filling it out while the file remains serenely still. A tiny `MOVED` stamp appears beside the unchanged object. Hold.
```


---

# Animation 35 — `curl` — transfer data with URLs

## Chapter 21 — Things From Elsewhere

## Source animation sequence

A tiny courier marked `curl` receives a URL.

It knocks on a server.

The server hands over a card:

`Location: elsewhere`

The courier goes elsewhere.

Another card.

Another address.

Again.

Again.

The courier eventually arrives back at the first building.

The server hands over one final card:

`Location: elsewhere`

The courier writes on the back:

**QUESTIONABLE RESEARCH METHOD**

## Sequence lock

**Background:** cobalt `#1C6094`

**Total duration:** 15 seconds (3 × 5-second cuts)

**Unique boundary frames:** 4

## Character / prop lock

COURIER_A: tiny running courier with rectangular satchel and round head; SERVER_BASE: identical simple building/server block cloned for every stop; REDIRECT_CARD_BASE: identical arrow card cloned. Courier design never changes while cards accumulate.

## Sequence Reference Board prompt

```text
Create a square 1:1 SEQUENCE REFERENCE BOARD for one Guide entry. This board is production reference and will not appear in the app.
Use the supplied 2005-film Guide screenshots only as visual-language references: original flat 2D vector / cut-paper / screen-printed pictograms, matte opaque geometry, economical anatomy, minimal faces and slightly imperfect print texture.
Place every recurring character and major prop from the CHARACTER LOCK below on one neutral sheet. For each recurring character show the SAME design in front view, side view and three-quarter view plus one neutral action pose. For clone bases show only the base design. For recurring props show one clean orthographic view.
Keep designs simple enough to reproduce exactly in every subsequent frame. Avoid tiny costume details that a video model will mutate.
The board itself may use small ID labels solely for production reference. Do not add entry titles or decorative UI.
This reference board is canonical. Subsequent frame/video generations must reproduce these exact designs rather than inventing variants.

CHARACTER LOCK
COURIER_A: tiny running courier with rectangular satchel and round head; SERVER_BASE: identical simple building/server block cloned for every stop; REDIRECT_CARD_BASE: identical arrow card cloned. Courier design never changes while cards accumulate.
```

## Shared video reference language

Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

## Boundary frame map

### Frame 00 — `35_curl_frame_00.png`

**Used as:** Cut 01 START

A tiny courier labeled `curl` receives a URL card and runs to a server building. The server does not hand over data; it hands over a card `Location: elsewhere` with an arrow.

### Frame 01 — `35_curl_frame_01.png`

**Used as:** Cut 01 END / Cut 02 START

End arriving at second server.

### Frame 02 — `35_curl_frame_02.png`

**Used as:** Cut 02 END / Cut 03 START

Path loops visibly toward the first building.

### Frame 03 — `35_curl_frame_03.png`

**Used as:** Cut 03 END

Loop diagram behind them.

## Cut 01 — 00:00–05:00

**START:** `35_curl_frame_00.png`

**END:** `35_curl_frame_01.png`

A tiny courier labeled `curl` receives a URL card and runs to a server building. The server does not hand over data; it hands over a card `Location: elsewhere` with an arrow. Courier pivots immediately and runs in that direction. End arriving at second server.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: cobalt #1C6094
Character/prop lock: COURIER_A: tiny running courier with rectangular satchel and round head; SERVER_BASE: identical simple building/server block cloned for every stop; REDIRECT_CARD_BASE: identical arrow card cloned. Courier design never changes while cards accumulate.
Reference board: use the canonical sequence reference board for Animation 35.
START FRAME: 35_curl_frame_00.png
END FRAME: 35_curl_frame_01.png

CUT 01 OF 03 — 00:00–05:00
A tiny courier labeled `curl` receives a URL card and runs to a server building. The server does not hand over data; it hands over a card `Location: elsewhere` with an arrow. Courier pivots immediately and runs in that direction. End arriving at second server.
```

## Cut 02 — 05:00–10:00

**START:** `35_curl_frame_01.png`

**END:** `35_curl_frame_02.png`

Second server gives another `Location` card. Then another server, another card, another direction. Use fast, crisp lateral map jumps rather than realistic travel. The courier becomes increasingly buried under redirect cards but remains dutiful. End as path loops visibly toward the first building.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: cobalt #1C6094
Character/prop lock: COURIER_A: tiny running courier with rectangular satchel and round head; SERVER_BASE: identical simple building/server block cloned for every stop; REDIRECT_CARD_BASE: identical arrow card cloned. Courier design never changes while cards accumulate.
Reference board: use the canonical sequence reference board for Animation 35.
START FRAME: 35_curl_frame_01.png
END FRAME: 35_curl_frame_02.png

CUT 02 OF 03 — 05:00–10:00
Second server gives another `Location` card. Then another server, another card, another direction. Use fast, crisp lateral map jumps rather than realistic travel. The courier becomes increasingly buried under redirect cards but remains dutiful. End as path loops visibly toward the first building.
```

## Cut 03 — 10:00–15:00

**START:** `35_curl_frame_02.png`

**END:** `35_curl_frame_03.png`

Courier arrives back at the original server from the opposite side. The server hands over one final card: `Location: elsewhere`. Courier turns the card over and writes `QUESTIONABLE RESEARCH METHOD`, then files it with the others rather than refusing. Hold on loop diagram behind them.

### Ready-to-paste video generation prompt

```text
Create one EXACTLY 5.0-second square Guide-animation cut.

REFERENCE LANGUAGE
- Use the supplied 2005-film Guide screenshots only as visual-language references: flat 2D vector / cut-paper / screen-printed pictograms, economical silhouettes, bold negative space, matte color, slightly imperfect printed edges, and dry diagrammatic staging. Create original compositions and original character designs.
- OUTPUT ASPECT RATIO: exactly 1:1. Compose for a square canvas from the beginning. No letterboxing and no cinematic crop.
- BACKGROUND: one single, flat, uninterrupted background color fills the entire square canvas from edge to edge. Use only the exact sequence background color supplied below. Do not create a second background band, top strip, bottom strip, colored edge, frame, border, matte, vignette, panel, window, chrome, or decorative perimeter.
- The HG2Gui app supplies all entry titles, tabs, chapter labels, framing and interface chrome. Therefore the animation itself must contain NO entry title, NO subject title, NO category tabs, NO header bar, NO Guide banner, NO decorative border, NO window controls and NO edge UI of any kind.
- Characters and props are flat opaque cutout shapes placed directly on the single-color background. No gradients, gloss, bloom, drop shadows, translucency, depth-of-field, photorealism or 3D rendering.
- People, aliens, files, machines and processes use deliberately economical geometric anatomy and minimal facial detail. Meaning comes from pose, scale, object relationships and necessary in-world labels rather than lip-sync or expressive acting.
- Perspective stays flattened. Diagrams may become rooms and labels may become physical objects. Motion is crisp and primarily linear: slides, pivots, hinges, flips, rotations, wipes, mechanical relabeling, flat pans and flat zooms. No camera shake, soft dissolve or bouncy modern easing.
- Comedy is dry. Nobody performs the joke for camera. The visual system simply follows the technical metaphor too literally.
- No generic outer space unless the cut specifically requires it. No neon cyberpunk, holographic HUD, glossy app interface, anime, 3D cartoon styling, cinematic lens effects, realistic terminal windows or desktop-window chrome.
- Do not invent dialogue, captions or labels beyond those explicitly required by the cut. Text that belongs to the app shell must never be painted into the animation.

CHARACTER / PROP CONTINUITY — MANDATORY
- Treat the supplied SEQUENCE REFERENCE BOARD as canonical model-sheet evidence, not inspiration. Every recurring character and prop must match it exactly: silhouette, head/body proportions, limb thickness, face marks, clothing/hat shape, color placement, prop geometry and relative scale.
- Every recurring element has a stable ID (for example USER_A, CLERK_A, FILE_A). Never redesign an ID because the pose, camera distance or scene changes. Only pose, position, orientation and allowed state may change.
- If several figures are defined as clones of one base ID, duplicate the same design exactly rather than inventing individuals.
- The provided START FRAME and END FRAME are hard visual constraints. Begin on the START FRAME composition and finish on the END FRAME composition. Animate the shortest clear transformation between them. Do not reinterpret either frame.
- The END FRAME of Cut N is literally the same image file as the START FRAME of Cut N+1. Never regenerate that shared boundary.
- When supported by the generator, attach the sequence reference board as a character/style reference in addition to the start and end frames. If a seed/model-version lock exists, keep it unchanged for every cut in the same entry, but the image references remain authoritative.

EDITING
- One major visual idea per five seconds.
- Land exactly on the supplied END FRAME by about 4.6 seconds and hold it through 5.0 seconds.
- No fade-in and no fade-out. No title card. No transition graphics. The app performs the surrounding presentation.

SEQUENCE LOCK
Background: cobalt #1C6094
Character/prop lock: COURIER_A: tiny running courier with rectangular satchel and round head; SERVER_BASE: identical simple building/server block cloned for every stop; REDIRECT_CARD_BASE: identical arrow card cloned. Courier design never changes while cards accumulate.
Reference board: use the canonical sequence reference board for Animation 35.
START FRAME: 35_curl_frame_02.png
END FRAME: 35_curl_frame_03.png

CUT 03 OF 03 — 10:00–15:00
Courier arrives back at the original server from the opposite side. The server hands over one final card: `Location: elsewhere`. Courier turns the card over and writes `QUESTIONABLE RESEARCH METHOD`, then files it with the others rather than refusing. Hold on loop diagram behind them.
```
