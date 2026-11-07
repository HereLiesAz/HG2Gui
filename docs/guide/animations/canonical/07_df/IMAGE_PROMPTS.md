# 07_df — image prompts

Four boundary frames for the three cuts of `07_df` (15 seconds), plus two model sheets. Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

The animation is the one already in the Guide (`animation.md`, `cut-sheet.md`); only the pictures are new. The concept is not changed here.

## The animation, as the Guide has it

A disk appears divided into USED and FREE. The FREE section notices the label. A tiny demonstration begins: FREE SPACE! FREE SPACE! A system administrator changes the sign to AVAILABLE. The demonstrators stare at it. One slowly lowers its placard. Another asks: "Available for what?" The administrator allocates the block. The demonstration ends for technical reasons.

## Look

Taken from the project's own reference frames of the 2005 film's Guide animations (`docs/guide/project/references/shared/`), as in `06_kill`:

- **Flat vector with simplified realism.** Realistically proportioned figures and objects reduced to a few large flat colour shapes, each with at most one darker flat tone for shadow.
- **Figures read almost as silhouettes** and are small in the frame. Faces are minimal: no eyes, no mouth, no wrinkles. Hair is one flat shape. Clothing is one flat colour with no folds, pockets, stitching or small accessories.
- **Flat colour field**, large areas of empty space, no outlines, no gradients, no texture. The disk is a flat diagram seen face-on.
- **No characters are borrowed** from other entries or from the reference frames.

## No lettering in the pictures

The animation is about words (USED, FREE, FREE SPACE!, AVAILABLE, "Available for what?"), and image generators misspell words. Every label, placard and sign in these prompts is a **blank card**. The text is composited in post, in the same step that adds the header and tabs. The pictures are 1:1 with no header and no tabs.

## The cast and set

- **The disk.** A large flat circle, seen face-on, split down the middle: the left half brick red (USED), the right half dark green (FREE). A small blank mustard tag sits on each half as its label.
- **The demonstrators.** Small people in the dark green half, each holding a blank cream placard on a stick. Four different individuals, each with a distinct silhouette:
  1. **The Marcher:** a tall woman in a mustard coat.
  2. **The Short One:** a short man in a dark green jacket.
  3. **The Quiet One:** a person in a cream sweater.
  4. **The Elder:** an old person in a navy coat, with a stoop.
- **The administrator.** A tall person in a navy suit, short hair, walking in from the side with one hand out.
- **The block.** One square section of the green half, outlined by its own darker tone, which can turn brick red.

## Where everything is, in each frame

| Frame | View | What is in it |
|---|---|---|
| 00 | wide | The disk at the centre of the cream field, left half brick red, right half dark green, a blank mustard tag on each half. Nothing else. |
| 01 | wide | The same disk. Six demonstrators in a line across the green half, placards raised, marching. At the right edge of the picture, the administrator has just stepped in. |
| 02 | wide | The same disk. The green half's tag is now a longer blank mustard tag. The demonstrators have stopped: one lowers its placard, one turns its placard over to look at the back. A blank cream caption card floats above one demonstrator. The administrator stands at the right edge. |
| 03 | wide | The same disk. One square block of the green half is brick red now, with one demonstrator standing inside it. The remaining demonstrators are walking away to the left edge. The green half is visibly smaller. The administrator stands at the right edge with one hand out toward the block. |

## Motion plan, for the video generation

- **Cut 1 (0–5 s).** The green half's tag lifts and turns as if it is reading itself. Small demonstrators appear in the green half, placards raised, and march neatly in a line. The administrator walks in from the right.
- **Cut 2 (5–10 s).** The administrator swaps the green half's tag for a longer one. The march stops. One demonstrator lowers its placard slowly; another turns its placard over to look at the back. A blank caption card appears above one demonstrator.
- **Cut 3 (10–15 s).** The administrator points to one block of the green half. The block turns brick red at once, with the demonstrator standing on it inside. The other demonstrators walk off the green half, and the picture holds on the smaller green region.

## Colours

Background for the whole animation: dirty cream `#C8BE9C`, from this entry's generated prompts. The palette is mustard `#D9A12B`, dirty cream, brick red `#8E3B2E`, dark green `#2F4A3A` and navy `#1C2A47`; the guide names these without hex values, so the hex values are my choices, as is the lighter cream `#E8DFC0` for placards. Lighter and darker flat tones of these are allowed for form and skin.

## How to use

1. Start one new ChatGPT conversation. Attach no reference images.
2. Paste the **opening message** once, and wait for the reply.
3. Generate **sheet A** (the people) and **sheet B** (the disk and props, attach sheet A). Check both before going on.
4. For every frame, attach both sheets and the previous approved frame (none for frame 00), paste the style block, then the frame prompt.
5. Save the sheets as `docs/07_df_sheet_a.png` and `docs/07_df_sheet_b.png` and the frames as `docs/07_df_frame_00.png` to `docs/07_df_frame_03.png`.

## Opening message (paste once, before the model sheets)

~~~text
I am going to ask you for two model sheets and then four consecutive frames of one short animated scene. Here is the story, so you know what each picture is for.

A disk is divided into USED and FREE. The FREE section notices its label. A tiny demonstration begins, with placards demanding free space. A system administrator changes the sign to AVAILABLE. The demonstrators stare at it. One slowly lowers its placard. Another asks what it is available for. The administrator allocates a block of the free space, and the demonstrator standing on it goes with it. The demonstration ends for technical reasons.

In the pictures, the disk is a flat circle seen face-on, brick red on the left (used) and dark green on the right (free). Every sign, tag and placard is a blank card: nothing in any picture is written, with no letters, no numbers and no labels. The words are added later.

Keep everything calm and deadpan. Nobody acts for the camera.

The style is flat vector illustration with simplified realism, in the manner of the animated Guide sequences of the 2005 film of The Hitchhiker's Guide to the Galaxy: realistically proportioned figures and objects reduced to a few large flat colour shapes, each with at most one darker flat tone for shadow. Figures are small in the frame and read almost as silhouettes: faces are minimal, with no eyes, no mouth, no wrinkles and no expression lines; hair is one flat shape; clothing is one flat colour with no folds, no pockets, no stitching and no accessories beyond the ones named. Keep each design identical in every image.

Reply "ready" and wait for the first request.
~~~

## Style block (paste before every sheet and frame prompt)

~~~text
Square image, 1:1. One single flat dirty cream (#C8BE9C) background fills the whole square edge to edge, with no texture and no grain. Flat vector illustration with simplified realism, in the manner of the animated Guide sequences of the 2005 film of The Hitchhiker's Guide to the Galaxy: realistically proportioned figures and objects reduced to a few large flat colour shapes, each with at most one darker flat tone for shadow. Figures are small in the frame and read almost as silhouettes: faces are minimal, with no eyes, no mouth, no wrinkles and no expression lines; hair is one flat shape; clothing is one flat colour with no folds, no pockets, no stitching and no accessories beyond the ones named. Large areas of empty space. No gradients, no gloss, no shading, no outlines, no texture, no painterly, anime or cartoon rendering, no 3D, no photographic detail. Palette: mustard, lighter cream, brick red, dark green, navy, each with one darker flat tone, and one flat skin tone. No text, no letters, no numbers, no labels, no signs with writing, no border, no frame, no header, no tabs. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Model sheets

### Model sheet A — the people

~~~text
A character model sheet on the flat dirty cream background. One row of five different people standing side by side, evenly spaced, full length, each with realistic proportions and posture, drawn as flat shapes with minimal faces. The first four each hold a blank lighter-cream placard on a stick: a tall woman in a mustard coat; a short man in a dark green jacket; a person in a cream sweater; an old, stooped person in a navy coat. The fifth, on the right, is a tall person in a navy suit with short hair, one hand held out, holding nothing. Nothing else. No text.
~~~

### Model sheet B — the disk and props (attach sheet A)

~~~text
A prop model sheet on the flat dirty cream background, in the same flat vector style with simplified realism as the attached sheet. Top row, two items: a large flat circle split down the middle, the left half brick red and the right half dark green, with a small blank mustard tag on each half; the same circle with one square block of the green half turned brick red. Bottom row, three items: a blank lighter-cream placard on a stick; a short blank mustard tag and a longer blank mustard tag side by side; a blank lighter-cream caption card. No text on anything.
~~~

## Frames

### Frame 00 — start of cut 01 · "A disk appears divided into USED and FREE."

Attach both sheets.

~~~text
Wide view. A large flat circle, seen face-on, sits at the centre of the dirty cream picture: its left half brick red, its right half dark green, with a small blank mustard tag on each half. Nothing else in the picture. No text.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "A tiny demonstration begins."

Attach both sheets and frame 00.

~~~text
Wide view of the same circle, left half brick red, right half dark green, with the small blank mustard tags. Six small people stand in a line across the green half, each holding a blank lighter-cream placard raised on a stick, drawn from the four different people on the model sheet: the woman in the mustard coat, the man in the dark green jacket, the person in the cream sweater, the stooped old person in the navy coat, and two of them repeated. At the right edge of the picture, the tall person in the navy suit has just stepped in. Nothing else. No text.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "Available for what?"

Attach both sheets and frame 01.

~~~text
Wide view of the same circle. The green half's tag is now a longer blank mustard tag. The six small people have stopped: one has lowered its placard to its side, another has turned its placard around to look at the back of it, the rest stand still. A blank lighter-cream caption card floats just above one of them. The tall person in the navy suit stands at the right edge. Nothing else. No text.
~~~

### Frame 03 — end of cut 03 · "The administrator allocates the block."

Attach both sheets and frame 02.

~~~text
Wide view of the same circle. One square block of the green half is brick red now, with one of the small people standing inside it holding a blank placard. The other five small people walk away along the left edge of the picture, away from the circle, placards lowered. The green half is visibly smaller. The tall person in the navy suit stands at the right edge with one hand out toward the red block. Nothing else. No text.
~~~

## Notes

- **No lettering.** Reject any frame with letters, numbers or signs with writing. The tags, placards and caption card are blank on purpose; the words are composited later.
- **Counting:** frames 01 and 02 have six small people and the administrator; frame 03 has one small person inside the red block, five walking away, and the administrator. Reject a frame that drops or duplicates someone.
- **Content versus defect:** when a frame departs on content (a different coat, a different placard), show it to the project owner rather than rejecting it. Reject only defects: wrong colour, non-flat background, gradients, 3D rendering, stray text, wrong count.
- **Invented by me, because the animation text does not say:** the disk's left/right split and colours, the four demonstrators and the administrator, the block, the camera (a fixed face-on view throughout), the frame compositions, and all the hex values.
