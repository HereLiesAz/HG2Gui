# HG2Gui Animation Production — NEXT SESSION HANDOFF

## READ THIS FIRST

We are continuing production on the animation assets for my project:

HG2Gui / The Hitchhiker’s Guide to Termux
https://github.com/HereLiesAz/HG2Gui

READ ALL ATTACHED MATERIAL BEFORE GENERATING ANYTHING.

We have 30+ Guide-entry animation sequences to complete. Do not redesign the project, rewrite the sequences, or invent a different production system.

### Current status

Sequence 01 — `pwd` has already been worked on and its generated images are acceptable. Do not redo it unless I explicitly ask.

Continue with:

Sequence 02 — `pkg install`

Then continue through the remaining sequences in order.

The animation text in the canonical animation sequence file is canonical. Preserve the intended visual joke and sequence of events.

---

# VISUAL TARGET

We are extending the visual language of the animated Hitchhiker’s Guide graphics created for the 2005 film.

Use the supplied movie screenshots as the primary visual references.

The desired visual language is:

- flat 2D animation;
- screen-printed / cut-paper / pictogram character;
- strong simplified silhouettes;
- intentionally economical anatomy;
- limited matte palette;
- flattened perspective;
- large areas of negative space;
- slightly imperfect printed texture;
- graphic rather than cinematic composition;
- visual metaphors treated literally;
- strange institutional/instructional calm;
- restrained character acting;
- no attempt at photorealism;
- no modern glossy motion-graphics aesthetic;
- no Pixar/Disney/cartoon rendering;
- no anime;
- no 3D characters;
- no neon cyberpunk;
- no holographic HUD;
- no glossy terminal windows;
- no cinematic depth-of-field, lens flare, bloom, or dramatic lighting.

Do not copy an existing movie shot or character. Continue the high-level visual grammar with original designs.

---

# ABSOLUTE FORMAT RULES

EVERY animation image is:

- EXACTLY 1:1 square composition.
- ONE SINGLE flat background color filling the entire image edge-to-edge.

The animation asset itself must contain NO app chrome.

HG2Gui supplies all of this separately.

Therefore NEVER generate:

- entry titles;
- chapter titles;
- subject headers;
- tab labels;
- category labels;
- colored header strips;
- colored side strips;
- frames;
- borders;
- mattes;
- letterboxing;
- interface chrome;
- Guide UI;
- decorative perimeter elements;
- video-player UI;
- production labels;
- timestamps;
- shot numbers;
- START FRAME / END FRAME text;
- character IDs;
- model-sheet labels;
- explanatory captions.

This point is critical.

Do not put production-reference information into the actual artwork.

If an object in the STORY legitimately contains text — a placard, command, sign, note, etc. — that text may appear.

Otherwise: no visible text.

---

# DO NOT MAKE STORYBOARD SHEETS

Never generate a storyboard page, contact sheet, production document, model-sheet page, sequence sheet, comic grid, or presentation containing multiple shots.

The image generator has previously interpreted words like “reference board,” “sequence,” and “cuts” as instructions to create production documents.

Do not do that.

Every image-generation call should request ONE ORDINARY SQUARE IMAGE.

Production organization happens outside the image.

---

# CHARACTER CONSISTENCY

Character consistency across the 5-second clips is one of the most important requirements.

Do NOT rely on prose alone to recreate the same character independently in every image.

Use an image-reference chain.

For each Guide entry:

1. Generate the FIRST canonical boundary frame.

2. Once that frame is approved, it becomes the visual source of truth for every recurring character and important prop in that entry.

3. Generate subsequent boundary frames by EDITING / CONTINUING FROM an already approved image whenever possible, rather than generating unrelated new images from scratch.

4. Preserve exactly:
   - head shape;
   - body proportions;
   - silhouette;
   - clothing;
   - color placement;
   - facial marks;
   - limb thickness;
   - prop geometry;
   - relative character scale.

5. Only change:
   - pose;
   - orientation;
   - location;
   - explicitly required story state.

6. When Cut N ends in a composition that begins Cut N+1, THE SAME IMAGE FILE is both:
   - Cut N END frame;
   - Cut N+1 START frame.

Do not regenerate shared boundaries.

This gives us N+1 images for N cuts, rather than 2N independently generated images.

If the image tool supports editing an existing generated image, USE EDITING aggressively for continuity.

If a later scene changes locations substantially, preserve the established characters by using an earlier approved image containing those characters as the reference.

---

# OPTIONAL CHARACTER REFERENCE IMAGE

A separate visual reference image may be generated when useful, BUT:

It must contain ONLY the actual characters/props on the sequence’s single flat background.

No visible:
- IDs;
- character names;
- labels;
- captions;
- title;
- grid;
- borders;
- annotation;
- production text.

Never invite the model to associate production labels with the animation artwork.

Internal names such as USER_A or CLERK_A may exist in our written notes/prompts only.

They must never appear in an image.

---

# BOUNDARY FRAME SYSTEM

Each animation sequence is divided into 5-second clips.

We need canonical still images at every boundary:

frame_00
frame_01
frame_02
frame_03
...

For a four-cut sequence:

Cut 1:
frame_00 -> frame_01

Cut 2:
frame_01 -> frame_02

Cut 3:
frame_02 -> frame_03

Cut 4:
frame_03 -> frame_04

The animation generator will later receive:

- the established character image/reference;
- the exact START boundary frame;
- the exact END boundary frame;
- the 5-second action description.

Our job in this session is primarily to produce those canonical images.

---

# HOW TO GENERATE EACH ENTRY

Work ONE Guide entry at a time.

Do not generate all 30 entries blindly.

For each entry:

A. Read its complete canonical animation sequence.

B. Determine how many 5-second clips it requires.

C. Determine the minimum number of unique boundary frames.

D. Choose ONE flat background color for that entire entry.

E. Generate frame_00 as a polished actual animation frame, NOT as production artwork.

F. Use that approved image as continuity evidence to generate frame_01.

G. Continue sequentially.

H. Keep every shared boundary literally identical.

I. When the entry is complete, provide clearly named files.

Then immediately proceed to the next entry unless something is clearly wrong.

---

# IMAGE-GENERATION PROMPT RULE

Prompts sent to the image generator should describe ONLY the picture it is supposed to make.

Do not contaminate the image-generation prompt with production-document language.

Bad:

“Create Sequence 2 character reference board with CUT 1 and frame IDs.”

Good:

“Square image. Flat oxblood background edge-to-edge. A small anthropomorphic parcel sits uneasily in a simple therapy chair. Three concerned parcel-people sit nearby. A tall narrow counsellor stands beside them holding a plain clipboard. Flat screen-printed cut-paper pictogram style…”

Do not tell the image generator about our filenames, cut numbers, production IDs, animation packet, or storyboard architecture unless absolutely necessary.

---

# STYLE DETAILS FROM THE MOVIE REFERENCES

Pay particular attention to:

- broad fields of one strong background color;
- strange flat characters that feel almost like public-information symbols;
- asymmetrical placement;
- unexpectedly empty areas;
- sparse props;
- deliberate shape language rather than illustrative detail;
- bold color relationships;
- slight paper/ink irregularity;
- absurd situations depicted with complete visual seriousness;
- poses that communicate instantly without needing dialogue;
- almost no conventional cinematic staging.

Characters should feel designed by the same graphic universe, but different Guide entries may use substantially different creatures or human forms when appropriate.

Consistency is required WITHIN an entry.

Uniformity ACROSS every entry is not required.

The original Guide graphics are eclectic.

---

# TECHNICAL TEXT

The source animation sequences sometimes require terminal commands or signs.

When exact text is narratively necessary, preserve it.

Do not add explanatory terminal output that is not in the canonical sequence.

Do not make a sequence more conventional merely because the command has a familiar real-world UI.

Example:

`pkg install` is NOT an animation about somebody typing into a terminal and watching a progress bar.

Its canonical joke is about software dependency being treated like personal dependency/intervention.

Follow the animation sequence, not the obvious software visualization.

---

# SEQUENCE 02 — `pkg install`

The canonical sequence is:

A small package sits in a chair.

Several concerned packages sit around it.

A counsellor asks:

“Do you feel you rely too heavily on libfoo?”

The package nods.

The counsellor turns to the door.

“Bring in libfoo.”

libfoo enters with three dependencies.

Those dependencies enter with nine more.

The room fills.

The walls bulge.

A forklift arrives carrying another room.

Terminal:

`Installing dependencies...`

The original package smiles for the first time.

This should remain a literal therapy/intervention joke.

Do NOT turn it into:
- warehouse logistics;
- package downloading;
- a computer operator installing boxes;
- filing cabinets;
- conventional terminal progress bars;
- delivery workers.

The innocent word being followed literally is DEPENDENCY.

The scene should look like the Guide calmly documenting an intervention that has accidentally become architecture.

Start by generating ONLY the actual first canonical animation frame for `pkg install`.

Requirements for that first frame:

- square 1:1;
- one flat background color edge-to-edge;
- no title;
- no header;
- no border;
- no labels;
- no captions;
- no production text;
- no terminal UI.

Image content:

A small anthropomorphic parcel sits uneasily in a simple therapy chair.

Several concerned parcel-like figures sit around it.

A tall, narrow counsellor stands nearby holding a plain clipboard.

The parcel characters must be visually simple enough to reproduce consistently through many later images.

Their geometry should be distinctive and memorable.

Use the supplied 2005 Guide screenshots as the visual-language reference.

Generate ONLY that image first.

After it exists, inspect it.

If it is good, continue frame-by-frame using it as the continuity reference.

Do not generate a commentary image, storyboard, model sheet, or production page.

We have more than 30 sequences to complete, so keep the workflow disciplined and keep moving.
