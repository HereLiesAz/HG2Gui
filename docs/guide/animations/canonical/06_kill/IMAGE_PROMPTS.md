# 06_kill — image prompts

Four boundary frames for the three cuts of `06_kill` (15 seconds), plus one model sheet. Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

Next in order after `05_top`. The concept is the guide's own and is kept as written: a post office sorts signals, one envelope cannot be refused, and everything else becomes terrifyingly cooperative. What is new is how it is drawn: from the project's existing Guide frames, with simple frames, short words and a planned camera.

## What the existing Guide frames teach

- **People are tall, elegant paper-cut figures**, with real proportions, a simple profile face, a tunic, a belt, trousers and boots, in a uniform and a cap. The clerk should look like the uniformed functionaries in `docs/guide/project/references/shared/` (`ls_clip_01_start.png`, `echo_clip_03_start.png`).
- **Plain staging.** Two to four things per frame, large, on one flat colour field with a little paper grain, and a few heavy props (a door, a filing cabinet, a table).
- **Diagram language.** Dashed arrows, sound arcs, and meaning carried by objects, not lettering.

## The story, in plain words

`kill` sends a signal to a process. Most signals only ask: terminate, hang up, stop, carry on. A process may catch them and reconsider its afternoon. One signal, `SIGKILL`, cannot be caught or ignored.

In the film this is a tiny post office. A clerk sorts envelopes, each a request: TERM (please terminate), HUP (hangup), STOP (don't move). Each goes to its chute and may be politely declined. At the far end of the bench sits a black envelope, KILL, which cannot be refused. The clerk approaches it with great procedural caution, using tongs. The moment he touches it, every other envelope in the room snaps into perfect, obedient order. Nothing explodes and nobody is hurt. It is just terrifying administrative cooperation, and the clerk looks mildly pleased with the improved workflow.

## The cast and set

- **The Clerk.** A tall thin man in the Guide's uniform: a black-red belted tunic, a cream shirt collar, a flat cap with a mustard band, black-red trousers and mustard boots. Profile face, a slightly tired expression. He never emotes.
- **The envelopes.** Cream rectangles with a flap, each with a coloured band and one short word in a heavy sans: **TERM** (muted cyan band), **HUP** (mustard band), **STOP** (black-red band). Each also has two or three thin lines of illegible small print. These are the only readable words in the scene, apart from the sign.
- **The black envelope.** Matte black with a thick black-red stamp that reads **KILL**. It always sits on a small cream cushion, so it does not vanish into the midnight blue background.
- **The set.** A long cream bench with a mustard top, a wall of pigeonholes behind it, three muted-cyan chutes in a row at the left, and a small hanging sign that reads **kill** in lowercase. At the far right end of the bench, a warning lamp over the cushion: a mustard lamp with a flat mustard cone of light.

## Camera plan

The camera moves, as the cut descriptions say.

- **Cut 1 (0–5 s):** fixed on the left half of the bench: the clerk, the chutes, the envelopes. At the end, a slow ease to the right begins as the clerk turns his head.
- **Cut 2 (5–10 s):** a slow pan right along the bench to the far end. The black envelope, the cushion and the lamp slide into the centre of the picture, and the clerk walks in from the left with a pair of long tongs.
- **Cut 3 (10–15 s):** a slow pull back to a wide view of the whole room, which reveals every other envelope now lined up in perfect rows.

## Where everything is, in each frame

| Frame | View | What is in it |
|---|---|---|
| 00 | left half | The Clerk at the bench, a TERM envelope in his hand over the left chute. HUP and STOP envelopes in a tray beside him. Three chutes, the pigeonholes, the hanging sign. No black envelope in view. |
| 01 | left half, camera easing right | Three envelopes have gone down three chutes. The Clerk has stopped, his head turned right. At the far edge, small and distant, the black envelope on its cushion under the lamp. |
| 02 | far right end, close | The black envelope and cushion large in the centre under the lamp's cone of light. The Clerk reaches in from the left with long tongs, the tips a finger's width above the envelope. Nobody else. |
| 03 | wide | The whole room. Every envelope, tray and chute aligned in perfect rows and columns, edges square. The black envelope on its cushion under the lamp at the right. The Clerk stands with his hands behind his back, mildly pleased. |

## Motion plan, for the video generation

- **Cut 1 (0–5 s).** The Clerk takes envelopes from the tray one at a time and posts each into its chute: TERM, HUP, STOP. Each slides down its chute and disappears. The camera is almost still. In the last second the Clerk stops, turns his head to the right, and the camera begins to ease right.
- **Cut 2 (5–10 s).** The camera pans right along the bench. The black envelope under the lamp slides into the centre of the picture. The Clerk walks into frame from the left, slowly, carrying a long pair of tongs, and stops with the tips just above the envelope. Hold the last half second.
- **Cut 3 (10–15 s).** The tongs touch the envelope. At once, in a single flat movement, every other envelope, tray and chute snaps into perfect alignment. The camera pulls back to show the whole room in rows. No explosion, no violence. The Clerk straightens up, puts his hands behind his back, and a very small smile appears. Hold.

## Colours

Background for the whole animation: midnight blue `#111D38`, from this entry's generated prompts. The palette is mustard, midnight blue, black-red, cream and muted cyan; the guide names these without hex values, so the hex values below are my choices: mustard `#D9A12B`, black-red `#5A1620`, cream `#F3E3B5`, muted cyan `#7FB5B8`.

## How to use

1. Start one new ChatGPT conversation. Attach the three style references from `docs/guide/project/references/shared/`: `ls_clip_01_start.png` (the man at the door), `echo_clip_03_start.png` (the bear and the men) and `echo_clip_01_start.png` (the megaphone woman). Say: "These are the Guide's own animation frames. Match this exact style: flat paper-cut figures with real proportions, simple profile faces, uniforms and boots, a flat colour field, a little paper grain. Do not copy their content."
2. Paste the **opening message** once, and wait for the reply.
3. Generate the **model sheet** and check it before going on.
4. For every frame, attach the model sheet and the previous approved frame (none for frame 00), paste the style block, then the frame prompt.
5. Save the sheet as `docs/06_kill_sheet.png` and the frames as `docs/06_kill_frame_00.png` to `docs/06_kill_frame_03.png`.

## Opening message (paste once, before the model sheet)

~~~text
I am going to ask you for one character and prop model sheet and then four consecutive frames of one short animated scene. Here is the story, so you know what each picture is for.

It is a tiny post office. A clerk sorts envelopes, each one a polite request: TERM asks someone to stop, HUP asks them to hang up, STOP tells them not to move. Each goes down its own chute and can be politely declined. At the far end of the bench sits one black envelope, KILL, which cannot be refused. The clerk approaches it very carefully with a pair of long tongs. The moment he touches it, every other envelope in the room snaps into perfect, obedient order. Nothing explodes and nobody is hurt: it is terrifying administrative cooperation, and the clerk looks mildly pleased with the improved workflow.

The camera moves: it starts on the left half of the bench, pans right to the black envelope, then pulls back to show the whole room.

Keep everything calm, flat and deadpan. Nobody acts for the camera.

The clerk is in the style of the attached Guide frames: a tall thin man, flat paper-cut, real proportions, simple profile face, in a black-red belted tunic with a cream collar, a flat cap with a mustard band, black-red trousers and mustard boots. The envelopes are cream with a flap and a coloured band and one short word in a heavy sans-serif, spelled exactly: TERM with a muted-cyan band, HUP with a mustard band, STOP with a black-red band, and each with two or three thin lines of illegible small print. The black envelope is matte black with a thick black-red stamp that reads exactly KILL, and it always sits on a small cream cushion. Keep each design identical in every image.

Reply "ready" and wait for the first request.
~~~

## Style block (paste before the sheet and every frame prompt)

~~~text
Square image, 1:1. One single flat midnight blue (#111D38) background fills the whole square edge to edge, with a very light paper grain. Flat 2D paper-cut illustration in the style of the attached Guide frames: tall elegant figures with real proportions, simple profile faces, solid flat colour shapes, slightly imperfect printed edges, large areas of empty space. No gradients, no gloss, no drop shadows, no 3D shading, no photographic detail. Only these colours besides the background: mustard, black-red, cream, muted cyan. The only lettering allowed is the short words named in the prompt, in a heavy sans-serif, spelled exactly as given, and nothing else. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Model sheet

~~~text
A model sheet on the flat midnight blue background. Top row: the clerk, a tall thin man in a black-red belted tunic with a cream collar, a flat cap with a mustard band, black-red trousers and mustard boots, in four poses, left to right: standing in profile with a cream envelope in his hand; walking carrying a long pair of tongs; reaching forward with the tongs held out at arm's length; standing with his hands behind his back and a very small smile. Bottom row: four envelopes laid out side by side, each cream with a flap and thin lines of illegible small print: one with a muted-cyan band and the word TERM; one with a mustard band and the word HUP; one with a black-red band and the word STOP; and, on a small cream cushion, a matte black envelope with a thick black-red stamp that reads KILL. Nothing else.
~~~

## Frames

### Frame 00 — start of cut 01 · "A tiny post office marked `kill` sorts envelopes."

Attach the model sheet.

~~~text
The left half of a tiny post office, drawn flat. A long cream bench with a mustard top runs across the picture. Behind it, a wall of small cream pigeonholes and a small hanging sign that reads kill in lowercase. At the left of the bench, three muted-cyan chutes in a row. The clerk from the attached sheet stands at the bench in profile, holding one cream envelope with a muted-cyan band and the word TERM above the left chute. Beside him on the bench, a small tray holds an envelope with a mustard band and the word HUP and an envelope with a black-red band and the word STOP. No black envelope in view. Nothing else.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "End with the clerk noticing a distant black envelope."

Attach the model sheet and frame 00.

~~~text
The same left half of the post office, the camera easing slightly to the right. The three muted-cyan chutes now each have an envelope sliding into them. The clerk has stopped, his hands empty, and his head is turned to the right, looking along the bench. At the far right edge of the picture, small and distant, a matte black envelope sits on a small cream cushion under a small mustard lamp. Nothing else.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "End with one hand just about to touch it."

Attach the model sheet and frame 01.

~~~text
Close view of the far right end of the same bench, the camera having panned along it. The matte black envelope with its thick black-red KILL stamp sits on its small cream cushion in the centre of the picture, under a mustard warning lamp that casts a flat mustard cone of light on it. From the left the clerk reaches in, in profile, holding a long pair of tongs at arm's length, the tips a finger's width above the envelope. Nothing else in the picture.
~~~

### Frame 03 — end of cut 03 · "Every other envelope … snaps into perfect obedient alignment … the clerk looking mildly pleased."

Attach the model sheet and frame 02.

~~~text
Wide view of the whole post office, the camera pulled back. Every envelope in the room is now lined up in perfect rows and columns with square edges on the bench and across the pigeonhole wall, and the three chutes stand perfectly straight. At the right, the matte black KILL envelope on its cream cushion under the mustard lamp, the tongs resting beside it. At the centre, the clerk stands in profile with his hands behind his back and a very small smile. Nothing else.
~~~

## Notes

- **Words:** only TERM, HUP, STOP, KILL and the lowercase sign kill, spelled exactly. The guide's longer lines (`TERM — REQUEST TO TERMINATE` and so on) are deliberately drawn as small illegible print: image generators misspell long text. If a short word still fails after three tries, stop and ask the project owner; the guide's own advice is to leave a clean area and add the exact words afterwards.
- **The black envelope** must stay visible against the dark background: it always has the cream cushion under it. Reject a frame where it vanishes.
- **Content versus defect:** when a frame departs on content (a different sign, an extra drawer), show it to the project owner rather than rejecting it. Reject only defects: wrong colour, non-flat background, broken shapes, misspelled words, wrong envelope count.
- **Invented by me, because the guide does not say:** the clerk's design, the cushion, the tongs, the pigeonholes, the lamp and its cone of light, the frame positions and the camera plan beyond "pans a little" and "hold". Reword freely.
