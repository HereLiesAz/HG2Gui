# 06_kill — image prompts

Four boundary frames for the three cuts of `06_kill` (15 seconds), plus two model sheets. Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

This replaces the earlier post-office version of the scene. The story is the project owner's; the cast and style come from the project's existing Guide frames in `docs/guide/project/references/shared/` (the `echo_clip_*`, `ls_clip_*` and `ls_tangent_clip_*` frames).

## The story, in plain words

In the beginning, God executed a command. As the process continued, files were saved, memories were stored, computations were powered, resources consumed, people began to wonder if maybe the command had gone on long enough, and tried to turn it off. People asked God politely to knock it off.

The French were about to execute the command when they remembered that's how God started the command in the first place. But that gave Friedrich Nietzsche an idea, and a signal was sent to kill. This, it did, a little too well.

The joke leans on one word: *execute* means both to run something and to put it to death. God started the command by executing it, so the French cannot execute it without confusing the two.

## What the existing Guide frames teach

- **People are tall, elegant paper-cut figures**, with real proportions, simple profile faces, uniforms, belts, caps and boots.
- **Plain staging.** Two to four things per frame, large, on one flat colour field with a little paper grain.
- **Diagram language.** Dashed arrows and sound arcs carry meaning, not lettering.

## No lettering at all

Nothing in this scene is written down: the keys are blank, the envelope is plain black, and nobody holds a sign. Image generators misspell text, and this scene does not need any.

## The cast and set

- **The Hand.** A giant flat cream hand coming down from the top edge, out of a black-red sleeve with a mustard cufflink, index finger extended. Only the hand and wrist are ever seen. It never moves except to press the first key and, at the end, to go limp.
- **The first key.** A large blank mustard computer key on a muted-cyan plinth.
- **The second key.** A smaller blank black-red key on its own small plinth, to the right of the first.
- **The machine.** A small round mustard machine with a big muted-cyan cogwheel on its side, a short chimney, a few small cream windows and a conveyor belt feeding it. Beside it: piles of cream folders (the files), muted-cyan jars (the memories) and mustard gears.
- **The crowd.** Five small figures in the Guide's uniform: black-red belted tunics, flat caps, black-red trousers, mustard boots. One steps forward holding his cap to his chest.
- **The French.** Two men in black-red berets, cream shirts with black-red horizontal stripes, black-red trousers and mustard boots. Small moustaches.
- **Nietzsche.** A tall man in a long black-red coat with a high cream collar, his hair brushed back and an enormous cream walrus moustache. He holds a plain black envelope.
- **The black envelope.** A plain matte black envelope, no text.

## Camera plan

The camera moves with each cut.

- **Cut 1 (0–5 s):** starts close on the Hand and the first key, then pulls back and tilts down to reveal the machine and the crowd far below.
- **Cut 2 (5–10 s):** a slow push in on the crowd, then a pan right to the French beside the second key, then back to the centre for Nietzsche's raised finger.
- **Cut 3 (10–15 s):** tilts up with the black envelope as it rises to the Hand, then settles on a wide view of the stopped machine.

## Where everything is, in each frame

| Frame | View | What is in it |
|---|---|---|
| 00 | close, top | The Hand with its finger a finger's width above the first key. Nothing else; empty midnight blue. |
| 01 | wide | The Hand at the top edge with the key pressed flat. Below, the machine turning, its cog mid-turn, the piles of folders, jars and gears growing beside it, the conveyor feeding it. At the bottom, the five uniformed figures with their faces tilted up. |
| 02 | medium, lower | The five figures in a loose crowd; one at the front with his cap held to his chest. At the right, the two French men reaching toward the second key and frozen. At the centre, Nietzsche stands with one finger raised. The machine runs in the background. The first key and the Hand at the top edge. |
| 03 | wide | The machine stopped: the cog still, the conveyor idle, the windows dark. The Hand hangs limp, fingers dangling. The crowd looks up. Nietzsche stands at the centre, mildly surprised, a thin dashed cream line from his hand rising toward the Hand. |

## Motion plan, for the video generation

- **Cut 1 (0–5 s).** The Hand descends and presses the first key. The key clicks down. Below, the machine starts to turn, the conveyor starts to feed it, and files, jars and gears begin to pile up beside it. The camera pulls back and tilts down. At the end, the small figures at the bottom tilt their faces up.
- **Cut 2 (5–10 s).** One figure steps forward, cap in hand, and bows politely, with a dashed cream arrow rising from him toward the Hand. The Hand does not move. The camera pans right: the two French men approach the second key, reach for it, glance up at the first key, and freeze. The camera returns to the centre and Nietzsche steps forward and raises one finger.
- **Cut 3 (10–15 s).** Nietzsche slides the black envelope into a slot on the plinth and a thin dashed cream line carries it up to the Hand as the camera tilts up. The Hand goes limp. The machine stops, then the conveyor, then the windows go dark one by one. No explosion. The camera settles wide. Hold on Nietzsche, mildly surprised, under the hanging Hand.

## Colours

Background for the whole animation: midnight blue `#111D38`, from this entry's generated prompts. The palette is mustard, midnight blue, black-red, cream and muted cyan; the guide names these without hex values, so the hex values below are my choices: mustard `#D9A12B`, black-red `#5A1620`, cream `#F3E3B5`, muted cyan `#7FB5B8`.

## How to use

1. Start one new ChatGPT conversation. Attach the three style references from `docs/guide/project/references/shared/`: `ls_clip_01_start.png` (the man at the door), `echo_clip_03_start.png` (the bear and the men) and `echo_clip_01_start.png` (the megaphone woman). Say: "These are the Guide's own animation frames. Match this exact style: flat paper-cut figures with real proportions, simple profile faces, uniforms and boots, a flat colour field, a little paper grain. Do not copy their content."
2. Paste the **opening message** once, and wait for the reply.
3. Generate **model sheet A** (the people) and **model sheet B** (the props, attach sheet A). Check both before going on.
4. For every frame, attach both sheets and the previous approved frame (none for frame 00), paste the style block, then the frame prompt.
5. Save the sheets as `docs/06_kill_sheet_a.png` and `docs/06_kill_sheet_b.png` and the frames as `docs/06_kill_frame_00.png` to `docs/06_kill_frame_03.png`.

## Opening message (paste once, before the model sheets)

~~~text
I am going to ask you for two model sheets and then four consecutive frames of one short animated scene. Here is the story, so you know what each picture is for.

In the beginning, God executed a command. It kept running, and files and memories piled up and resources were used, and people began to wonder whether it had gone on long enough. They asked God, politely, to stop. God ignored them. Two Frenchmen were about to execute the command, which can also mean to put it to death, when they remembered that executing it was how God had started it in the first place. A tall man with an enormous moustache, Friedrich Nietzsche, had an idea. He posted a black envelope up to God. The command stopped, and then everything else did, a little too well.

In the pictures, God is only a giant hand coming down from the top of the picture. The command is a small round machine. Nothing in any picture is written: no letters, no numbers, no signs, no labels.

The camera moves between frames: it starts close on the hand, pulls back and tilts down to the machine and a small crowd, pushes in on the crowd, then tilts up to the hand and settles on a wide view.

Keep everything calm, flat and deadpan. Nobody acts for the camera.

The characters are in the style of the attached Guide frames: flat paper-cut figures with real proportions and simple profile faces. The hand is cream, from a black-red sleeve with a mustard cufflink. The crowd are five men in black-red belted tunics, flat caps, black-red trousers and mustard boots. The two Frenchmen wear black-red berets, cream shirts with black-red horizontal stripes, black-red trousers, mustard boots and small moustaches. Nietzsche is tall, in a long black-red coat with a high cream collar, hair brushed back, with an enormous cream walrus moustache. Keep each design identical in every image.

Reply "ready" and wait for the first request.
~~~

## Style block (paste before every sheet and frame prompt)

~~~text
Square image, 1:1. One single flat midnight blue (#111D38) background fills the whole square edge to edge, with a very light paper grain. Flat 2D paper-cut illustration in the style of the attached Guide frames: tall elegant figures with real proportions, simple profile faces, solid flat colour shapes, slightly imperfect printed edges, large areas of empty space. No gradients, no gloss, no drop shadows, no 3D shading, no photographic detail. Only these colours besides the background: mustard, black-red, cream, muted cyan. No text, no letters, no numbers, no labels, no signs, no border, no frame. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Model sheets

### Model sheet A — the people

~~~text
A character model sheet on the flat midnight blue background. One row of four full-length characters standing side by side in profile, facing slightly to the right, evenly spaced, in the style of the attached Guide frames. From left to right: a man in a black-red belted tunic, a flat cap, black-red trousers and mustard boots, holding his cap to his chest; two Frenchmen standing together, both in black-red berets, cream shirts with black-red horizontal stripes, black-red trousers, mustard boots and small moustaches, one with his hand reaching forward; a tall thin man in a long black-red coat with a high cream collar, hair brushed back, with an enormous cream walrus moustache, one finger raised, a plain black envelope in his other hand. Nothing else. No text.
~~~

### Model sheet B — the props (attach sheet A)

~~~text
A prop model sheet on the flat midnight blue background, in the same flat paper-cut style as the attached sheet. Top row, three items: a giant flat cream hand and wrist coming out of a black-red sleeve with a mustard cufflink, the index finger extended; a large blank mustard computer key on a muted-cyan plinth; a smaller blank black-red key on a smaller muted-cyan plinth. Bottom row, three items: a small round mustard machine with a big muted-cyan cogwheel on its side, a short chimney, a few small cream windows and a conveyor belt feeding it; beside it a pile of cream folders, a row of muted-cyan jars and a heap of mustard gears; and a plain matte black envelope. No text on anything.
~~~

## Frames

### Frame 00 — start of cut 01 · "A midnight-blue void. A hand the size of a continent presses a single large key."

Attach both model sheets.

~~~text
Close view. A giant flat cream hand and wrist comes down from the top edge of the picture out of a black-red sleeve with a mustard cufflink, its index finger extended and pointing straight down. A finger's width below the fingertip, a large blank mustard computer key sits on a muted-cyan plinth at the bottom centre. Nothing else in the picture: empty midnight blue all around. No text.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "A small machine begins to turn … the piles growing, and a crowd of small figures … beginning to look up."

Attach both model sheets and frame 00.

~~~text
Wide view, the camera pulled back and tilted down. At the top edge, the giant cream hand with its finger pressing the large blank mustard key down flat on its plinth. Far below, the small round mustard machine with the muted-cyan cog on its side is turning, a conveyor belt feeding it, with a pile of cream folders, a row of muted-cyan jars and a heap of mustard gears growing beside it. At the very bottom, five small men in black-red belted tunics and flat caps stand with their faces tilted up. Nothing else. No text.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "End on the raised finger."

Attach both model sheets and frame 01.

~~~text
Medium view of the lower part of the same scene. The five small men in black-red tunics stand in a loose crowd, one at the front with his flat cap held to his chest. At the right, two Frenchmen in black-red berets and striped shirts reach toward a smaller blank black-red key on its small plinth and have frozen with their hands out, looking up. At the centre, a tall man in a long black-red coat with a high cream collar and an enormous cream walrus moustache stands in profile with one finger raised. Behind them the small mustard machine still runs. At the very top edge, the large mustard key and the bottom of the giant cream hand. Nothing else. No text.
~~~

### Frame 03 — end of cut 03 · "The hand goes limp. The machine stops … Hold."

Attach both model sheets and frame 02.

~~~text
Wide view. The small round mustard machine is stopped: the cog is still, the conveyor is idle and its small windows are dark. At the top edge, the giant cream hand hangs limp, its fingers dangling. The five small men in black-red tunics and the two Frenchmen stand in a loose crowd with their faces tilted up. At the centre, the tall man in the long black-red coat with the enormous cream moustache stands in profile looking mildly surprised, his hand lowered, and a thin dashed cream line fades upward from his hand toward the hanging hand. Nothing else. No text.
~~~

## Notes

- **No lettering.** Reject any frame with letters, numbers, signs or labels. The keys and the envelope are blank on purpose.
- **Counting:** frame 01 has five small men; frame 02 has five men, two Frenchmen and Nietzsche; frame 03 has the same eight. Reject a frame that drops or duplicates someone.
- **If frame 02 is crowded,** make it in two passes: first Nietzsche with his finger raised and the crowd, then add the two Frenchmen and the second key.
- **Content versus defect:** when a frame departs on content (a different moustache, an extra jar), show it to the project owner rather than rejecting it. Reject only defects: wrong colour, non-flat background, broken shapes, stray text, wrong count.
- **Invented by me, because the story does not say:** the Hand as God, the keys, the machine and its piles, the five-man crowd, the Frenchmen's striped shirts, Nietzsche's coat and moustache, the black envelope, the camera plan and the frame positions. Reword freely.
