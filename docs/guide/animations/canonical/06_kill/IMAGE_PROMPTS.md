# 06_kill — image prompts

Four boundary frames for the three cuts of `06_kill` (15 seconds), plus two model sheets. Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

This replaces the earlier post-office version of the scene. The story is the project owner's; the cast and style come from the project's existing Guide frames in `docs/guide/project/references/shared/` (the `echo_clip_*`, `ls_clip_*` and `ls_tangent_clip_*` frames).

## The story, in plain words

In the beginning, God executed a command. As the process continued, files were saved, memories were stored, computations were powered, resources consumed, people began to wonder if maybe the command had gone on long enough, and tried to stop it. People asked God politely to knock it off, which he ignored, as per usual.

They were about to execute the command when they remembered that's how God started the command in the first place. But that gave Friedrich Nietzsche an idea, and a signal was sent to kill it. This, it did, a little too well.

The joke leans on one word: *execute* means both to run something and to put it to death. God started the command by executing it, so anyone about to execute it has to stop and work out which meaning they meant.

## Look

Taken from the project's own reference frames of the 2005 film's Guide animations (`docs/guide/project/references/shared/`, the `screencapture-...guillotine...` frames), not from any earlier prompt of mine:

- **Flat vector with simplified realism.** Realistically proportioned figures and objects reduced to a few large flat colour shapes, each with at most one darker flat tone for shadow.
- **Figures read almost as silhouettes** and are small in the frame. Faces are minimal: no eyes, no mouth, no wrinkles. Hair is one flat shape. Clothing is one flat colour with no folds, pockets, stitching or small accessories.
- **Dark, saturated colour fields**, large areas of empty space, no outlines, no gradients, no texture.
- **What is wrong, from a rejected render:** painted anime-style faces, shaded clothing folds, tool belts and other fine detail. That is detail, not simplified realism.

## No lettering at all

Nothing in this scene is written down: the keys are blank, the envelope is plain black, and nobody holds a sign. Image generators misspell text, and this scene does not need any.

## The cast and set

- **The Hand.** A giant, realistically proportioned human hand and wrist coming down from the top edge, out of a black-red cuffed sleeve with a mustard cufflink, index finger extended. Only the hand and wrist are ever seen. It never moves except to press the first key and, at the end, to go limp.
- **The planetoid.** A small rocky planetoid, seen as a curved horizon floating in the dark, where everything below the Hand happens.
- **The first key.** A large blank mustard computer key on a muted-cyan base.
- **The second key.** A smaller blank black-red key on its own small base, to the right of the first.
- **The machine.** A compact industrial machine, round and mustard, with a big muted-cyan cogwheel on its side, a short chimney, a few lit windows and a conveyor belt feeding it, standing on the planetoid. Beside it: piles of cream folders (the files), muted-cyan glass jars (the memories) and mustard gears.
- **The crowd.** Four different people, each a distinct individual with realistic proportions and a distinct silhouette:
  1. **The Asker:** a tall, thin man in a long cream coat, holding a small black-red hat in both hands against his chest.
  2. **The Mechanic:** a short, broad woman in mustard overalls, her hair tied back.
  3. **The Elder:** an old, bald, stooped man in a long muted-cyan robe, leaning on a walking stick.
  4. **The Young One:** a teenager in a black-red jacket with a mustard backpack.
- **The Pair.** Two identical tall people in matching muted-cyan coveralls, with short hair, who go for the second key.
- **Nietzsche.** Tall and thin, in a long black-red coat with a high cream collar, hair brushed back, with an enormous walrus moustache. He holds a plain black envelope.
- **The black envelope.** A plain matte black envelope, no text.

## Camera plan

The camera moves with each cut.

- **Cut 1 (0–5 s):** starts close on the Hand and the first key, then pulls back and tilts down to reveal the planetoid, the machine and the crowd far below.
- **Cut 2 (5–10 s):** a slow push in on the crowd, then a pan right to the Pair beside the second key, then back to the centre for Nietzsche's raised finger.
- **Cut 3 (10–15 s):** tilts up with the black envelope as it rises to the Hand, then settles on a wide view of the stopped machine.

## Where everything is, in each frame

| Frame | View | What is in it |
|---|---|---|
| 00 | close, top | The Hand with its finger a finger's width above the first key. Nothing else; empty midnight blue. |
| 01 | wide | The Hand at the top edge with the key pressed flat. Below, the planetoid with the machine turning, its cog mid-turn, the piles of folders, jars and gears growing beside it, the conveyor feeding it. At the bottom, the four people of the crowd with their faces tilted up. |
| 02 | medium, lower | The four people in a loose group; the Asker at the front with his hat held to his chest. At the right, the Pair reaching toward the second key and frozen. At the centre, Nietzsche stands with one finger raised. The machine runs in the background. The first key and the Hand at the top edge. |
| 03 | wide | The machine stopped: the cog still, the conveyor idle, the windows dark. The Hand hangs limp, fingers dangling. The crowd looks up. Nietzsche stands at the centre, mildly surprised, a thin dashed cream line from his hand rising toward the Hand. |

## Motion plan, for the video generation

- **Cut 1 (0–5 s).** The Hand descends and presses the first key. The key clicks down. On the planetoid below, the machine starts to turn, the conveyor starts to feed it, and files, jars and gears begin to pile up beside it. The camera pulls back and tilts down. At the end, the people at the bottom tilt their faces up.
- **Cut 2 (5–10 s).** The Asker steps forward, hat held to his chest, and bows politely, with a dashed cream arrow rising from him toward the Hand. The Hand does not move. The camera pans right: the Pair approach the second key, reach for it, glance up at the first key, and freeze. The camera returns to the centre and Nietzsche steps forward and raises one finger.
- **Cut 3 (10–15 s).** Nietzsche slides the black envelope into a slot on the base and a thin dashed cream line carries it up to the Hand as the camera tilts up. The Hand goes limp. The machine stops, then the conveyor, then the windows go dark one by one. No explosion. The camera settles wide. Hold on Nietzsche, mildly surprised, under the hanging Hand.

## Colours

Background for the whole animation: midnight blue `#111D38`, from this entry's generated prompts. The palette is mustard, midnight blue, black-red, cream and muted cyan; the guide names these without hex values, so the hex values below are my choices: mustard `#D9A12B`, black-red `#5A1620`, cream `#F3E3B5`, muted cyan `#7FB5B8`. Lighter and darker flat tones of these colours are allowed for form and skin.

## How to use

1. Start one new ChatGPT conversation. Attach no reference images: nothing from the other entries is to be copied.
2. Paste the **opening message** once, and wait for the reply.
3. Generate **sheet A** (the crowd), **sheet B** (the Pair and Nietzsche) and **sheet C** (the props). Check all three before going on.
4. For every frame, attach the three sheets and the previous approved frame (none for frame 00), paste the style block, then the frame prompt.
5. Save the sheets as `docs/06_kill_sheet_a.png`, `docs/06_kill_sheet_b.png` and `docs/06_kill_sheet_c.png` and the frames as `docs/06_kill_frame_00.png` to `docs/06_kill_frame_03.png`.

## Opening message (paste once, before the model sheets)

~~~text
I am going to ask you for three model sheets and then four consecutive frames of one short animated scene. Here is the story, so you know what each picture is for.

In the beginning, God executed a command. As the process continued, files were saved, memories were stored, computations were powered, resources consumed, people began to wonder if maybe the command had gone on long enough, and tried to stop it. People asked God politely to knock it off, which he ignored, as per usual. They were about to execute the command when they remembered that's how God started the command in the first place. But that gave Friedrich Nietzsche an idea, and a signal was sent to kill it. This, it did, a little too well. (Execute means both to run something and to put it to death.)

In the pictures, God is only a giant hand coming down from the top of the picture. The command is a small round machine standing on a small planetoid floating in the dark. The people are a small crowd of ordinary individuals. The signal is a plain black envelope that Nietzsche posts up to God. Nothing in any picture is written: no letters, no numbers, no signs, no labels.

The camera moves between frames: it starts close on the hand, pulls back and tilts down to the planetoid and the crowd, pushes in on the crowd, then tilts up to the hand and settles on a wide view.

Keep everything calm and deadpan. Nobody acts for the camera.

The style is flat vector illustration with simplified realism, in the manner of the animated Guide sequences of the 2005 film of The Hitchhiker's Guide to the Galaxy: realistically proportioned figures and objects reduced to a few large flat colour shapes, each with at most one darker flat tone for shadow. Figures are small in the frame and read almost as silhouettes: faces are minimal, with no eyes, no mouth, no wrinkles and no expression lines; hair is one flat shape; clothing is one flat colour with no folds, no pockets, no stitching and no accessories beyond the ones named. Keep each design identical in every image.

Reply "ready" and wait for the first request.
~~~

## Style block (paste before every sheet and frame prompt)

~~~text
Square image, 1:1. One single flat midnight blue (#111D38) background fills the whole square edge to edge, with no texture and no grain. Flat vector illustration with simplified realism, in the manner of the animated Guide sequences of the 2005 film of The Hitchhiker's Guide to the Galaxy: realistically proportioned figures and objects reduced to a few large flat colour shapes, each with at most one darker flat tone for shadow. Figures are small in the frame and read almost as silhouettes: faces are minimal, with no eyes, no mouth, no wrinkles and no expression lines; hair is one flat shape; clothing is one flat colour with no folds, no pockets, no stitching and no accessories beyond the ones named. Large areas of empty space. No gradients, no gloss, no shading, no outlines, no texture, no painterly, anime or cartoon rendering, no 3D, no photographic detail. Palette: mustard, black-red, cream, muted cyan, each with one darker flat tone, and one flat skin tone. No text, no letters, no numbers, no labels, no signs, no border, no frame. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Model sheets

### Model sheet A — the crowd

~~~text
A character model sheet on the flat midnight blue background. One row of four different people standing side by side, evenly spaced, full length, each with realistic proportions and posture, drawn as flat shapes with minimal faces. From left to right: a tall, thin man in a long cream coat, holding a small black-red hat in both hands against his chest; a short, broad woman in mustard overalls, her hair tied back; an old, bald, stooped man in a long muted-cyan robe, leaning on a walking stick; a teenager in a black-red jacket with a mustard backpack. Nothing else. No text.
~~~

### Model sheet B — the Pair and Nietzsche (attach sheet A)

~~~text
A character model sheet on the flat midnight blue background, in the same flat vector style with simplified realism as the attached sheet. A row of three full-length people standing side by side, evenly spaced, each with realistic proportions and posture, drawn as flat shapes with minimal faces. On the left, two identical tall people with short hair, standing close together in matching muted-cyan coveralls, one of them reaching a hand forward. On the right, one tall thin man in profile, in a long black-red coat with a high cream collar, hair brushed back, with an enormous walrus moustache, one finger raised and a plain black envelope in his other hand. Nothing else. No text.
~~~

### Model sheet C — the props (attach sheets A and B)

~~~text
A prop model sheet on the flat midnight blue background, in the same flat vector style with simplified realism as the attached sheets. Top row, three items: a giant realistically proportioned human hand and wrist coming out of a black-red cuffed sleeve with a mustard cufflink, the index finger extended; a large blank mustard computer key on a muted-cyan base; a smaller blank black-red key on a smaller muted-cyan base. Bottom row, three items: a small rocky planetoid seen as a curved horizon, with a compact round mustard industrial machine standing on it, with a big muted-cyan cogwheel on its side, a short chimney, a few lit windows and a conveyor belt feeding it, and beside it a pile of cream folders, a row of muted-cyan glass jars and a heap of mustard gears; and a plain matte black envelope. No text on anything.
~~~

## Frames

### Frame 00 — start of cut 01 · "A midnight-blue void. A hand the size of a continent presses a single large key."

Attach the three sheets.

~~~text
Close view. A giant, realistically proportioned human hand and wrist comes down from the top edge of the picture out of a black-red cuffed sleeve with a mustard cufflink, its index finger extended and pointing straight down. A finger's width below the fingertip, a large blank mustard computer key sits on a muted-cyan base at the bottom centre. Nothing else in the picture: empty midnight blue all around. No text.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "A small machine begins to turn … the piles growing, and a crowd of small figures … beginning to look up."

Attach the three sheets and frame 00.

~~~text
Wide view, the camera pulled back and tilted down. At the top edge, the giant hand with its finger pressing the large blank mustard key down flat on its base. Far below, a small rocky planetoid with a curved horizon, and on it the compact round mustard machine with the muted-cyan cog on its side, turning, a conveyor belt feeding it, with a pile of cream folders, a row of muted-cyan glass jars and a heap of mustard gears growing beside it. At the very bottom, the four people from the model sheet stand with their faces tilted up: the tall thin man in the cream coat with his hat, the broad woman in mustard overalls, the stooped old man in the cyan robe with his stick, the teenager in the black-red jacket with the mustard backpack. Nothing else. No text.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "End on the raised finger."

Attach the three sheets and frame 01.

~~~text
Medium view of the lower part of the same scene. The four people stand in a loose group, the tall thin man in the cream coat at the front with his small hat held to his chest. At the right, the two identical people in muted-cyan coveralls reach toward a smaller blank black-red key on its small base and have frozen with their hands out, looking up. At the centre, the tall thin man in the long black-red coat with the high cream collar and the enormous walrus moustache stands in profile with one finger raised. Behind them the compact mustard machine still runs. At the very top edge, the large mustard key and the bottom of the giant hand. Nothing else. No text.
~~~

### Frame 03 — end of cut 03 · "The hand goes limp. The machine stops … Hold."

Attach the three sheets and frame 02.

~~~text
Wide view. The compact mustard machine is stopped: the cog is still, the conveyor is idle and its windows are dark. At the top edge, the giant hand hangs limp, its fingers dangling. The four people and the two identical people in muted-cyan coveralls stand in a loose group with their faces tilted up. At the centre, the tall thin man in the long black-red coat with the enormous walrus moustache stands in profile looking mildly surprised, his hand lowered, and a thin dashed cream line fades upward from his hand toward the hanging hand. Nothing else. No text.
~~~

## Notes

- **No lettering.** Reject any frame with letters, numbers, signs or labels. The keys and the envelope are blank on purpose.
- **Counting:** frame 01 has four people; frame 02 has four people, the Pair and Nietzsche; frame 03 has the same seven. Reject a frame that drops or duplicates someone.
- **If frame 02 is crowded,** make it in two passes: first Nietzsche with his finger raised and the four people, then add the Pair and the second key.
- **Content versus defect:** when a frame departs on content (a different hat, an extra jar), show it to the project owner rather than rejecting it. Reject only defects: wrong colour, non-flat background, gradients, 3D rendering, stray text, wrong count.
- **Invented by me, because the story does not say:** the Hand as God, the keys, the planetoid, the machine and its piles, the four people of the crowd, the Pair, Nietzsche's coat and moustache, the black envelope, the camera plan and the frame positions. Reword freely.
