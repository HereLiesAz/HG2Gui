# 06_kill — image prompts

Four boundary frames for the three cuts of `06_kill` (15 seconds), three optional extra frames for the fast first cut, and three model sheets. Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

This is the second version of the animation. It replaces the earlier machine, key and envelope version. The story is the project owner's; the look comes from the project's own reference frames of the 2005 film's Guide animations in `docs/guide/project/references/shared/`.

## The story, in plain words

In the beginning, God executed a command. As the process continued, files were saved, memories were stored, computations were powered, resources consumed, people began to wonder if maybe the command had gone on long enough, and tried to stop it. People asked God politely to knock it off, which he ignored, as per usual. They were about to execute the command when they remembered that's how God started the command in the first place. But that gave Friedrich Nietzsche an idea, and a signal was sent to kill it. This, it did, a little too well.

## The animation, as the owner described it

The big hand presses a button, and a brown planet pops up. A lot of water pours onto it, and the camera zooms in to a desolate desert beach. Trees pop up, then a village, then a city, then more and more buildings crowd the screen. Wars break out and destroy some of the buildings, more buildings replace them, and the air fills with smog. Together with the smog and the buildings popping up, the screen goes black.

The people who ask God politely to stop walk in and plead. The giant hand comes down from the top of the screen and gives them the middle finger. Two characters wheel out a guillotine, pause while looking up, and wheel it back off the screen.

Nietzsche walks on, puts a finger in the air as if he has an idea, puts his hand in his pocket and pulls out a gun. He aims it at the top of the screen and pulls the trigger. The giant hand falls from out of view and lands behind him, dead.

## Look

Taken from the project's own reference frames of the 2005 film's Guide animations (`docs/guide/project/references/shared/`, the `screencapture-...guillotine...` frames), not from any earlier prompt of mine:

- **Flat vector with simplified realism.** Realistically proportioned figures and objects reduced to a few large flat colour shapes, each with at most one darker flat tone for shadow.
- **Figures read almost as silhouettes** and are small in the frame. Faces are minimal: no eyes, no mouth, no wrinkles. Hair is one flat shape. Clothing is one flat colour with no folds, pockets, stitching or small accessories.
- **Dark, saturated colour fields**, large areas of empty space, no outlines, no gradients, no texture.
- **What is wrong, from a rejected render:** painted anime-style faces, shaded clothing folds, tool belts and other fine detail. That is detail, not simplified realism.

## No lettering at all

Nothing in this scene is written down: the button is blank, nobody holds a sign. Image generators misspell text, and this scene does not need any.

## The cast and set

- **The Hand.** A giant, realistically proportioned human hand and wrist coming down from the top edge, out of a black-red cuffed sleeve with a mustard cufflink. Only the hand and wrist are ever seen. It presses the button with its index finger, later gives the middle finger, and at the end falls and lies dead.
- **The button.** A large round blank mustard button on a muted-cyan base.
- **The planet.** A round brown planet. Water (muted cyan) pours onto it and leaves a desert shoreline.
- **The beach, the trees, the village, the city.** Seen only in the first cut: a desolate sand beach with a calm sea, then bare trees, a few small houses, then ever denser buildings. Buildings are flat slabs of mustard, cream and black-red. Some are broken by explosions. Smog is a dark grey-brown haze that thickens until the picture is black.
- **The crowd.** Four different people, each a distinct individual with realistic proportions and a distinct silhouette:
  1. **The Asker:** a tall, thin man in a long cream coat, holding a small black-red hat in both hands against his chest.
  2. **The Mechanic:** a short, broad woman in mustard overalls, her hair tied back.
  3. **The Elder:** an old, bald, stooped man in a long muted-cyan robe, leaning on a walking stick.
  4. **The Young One:** a teenager in a black-red jacket with a mustard backpack.
- **The Pair.** Two identical tall people in matching muted-cyan coveralls, with short hair, who wheel the guillotine.
- **The guillotine.** A tall upright black-red frame on a small wheeled cart, with a cream blade at the top and a basket at the front.
- **Nietzsche.** Tall and thin, in a long black-red coat with a high cream collar, hair brushed back, with an enormous walrus moustache. He raises one finger, then pulls a small black revolver from his coat pocket.

## Camera plan

- **Cut 1 (0–5 s):** starts close on the Hand and the button, pulls back as the planet pops up and the water pours, then zooms in fast to the beach and holds a fixed view while the trees, village and city time-lapse until the screen goes black.
- **Cut 2 (5–10 s):** a fixed medium view of a bare stage. The crowd walks in and pleads, the Hand comes down from the top edge, the Pair wheel the guillotine across and back.
- **Cut 3 (10–15 s):** the same stage. Nietzsche walks in, the camera tilts up with his aim, then back down as the Hand falls in behind him.

## Where everything is, in each frame

| Frame | View | What is in it |
|---|---|---|
| 00 | close, top | The Hand with its index finger a finger's width above the button. Nothing else; empty midnight blue. |
| E1 (optional) | wide | The brown planet floating in the dark, a thick stream of water pouring onto it from the top. |
| E2 (optional) | wide | A desolate desert beach: pale sand, a calm sea, a single bare tree. |
| E3 (optional) | wide | A dense city with several buildings broken by explosions, thick smog rising. |
| 01 | wide | The whole picture is crowded with buildings under thick smog, so dark it is almost black. |
| 02 | medium | A bare stage. The four people stand in a loose group looking up. At the top edge, the Hand's fist with the middle finger raised. |
| 03 | wide | Nietzsche at the centre, gun lowered. Behind him the Hand lies on its back on the ground, dead, fingers curled. The four people stand at the edges. |

## Motion plan, for the video generation

- **Cut 1 (0–5 s).** The Hand descends and presses the button, which clicks down. A brown planet pops up. Water pours onto it. The camera zooms in to a desolate desert beach. Trees pop up, then a village, then a city, then more and more buildings crowd the screen. Wars break out and destroy some of the buildings, and new buildings replace them. Smog fills the air. Between the smog and the buildings the screen goes black.
- **Cut 2 (5–10 s).** The four people walk onto the bare stage and plead, the Asker bowing with his hat held out. The Hand comes down from the top edge and gives them the middle finger. The Pair wheel the guillotine in from one side, stop, look up, and wheel it back off the same side.
- **Cut 3 (10–15 s).** Nietzsche walks on and raises one finger as if he has an idea, then puts his hand in his coat pocket and pulls out the revolver. He aims at the top of the screen and fires. The Hand falls from out of view and lands behind him, dead. Hold.

## Colours

Background for the whole animation: midnight blue `#111D38`. The palette is mustard, midnight blue, black-red, cream and muted cyan; the hex values below are my choices: mustard `#D9A12B`, black-red `#5A1620`, cream `#F3E3B5`, muted cyan `#7FB5B8`, planet brown `#6B4A2F`, smog `#2A2018`. Lighter and darker flat tones of these are allowed for form and skin.

## How to use

1. Start one new ChatGPT conversation. Attach no reference images: nothing from the other entries is to be copied.
2. Paste the **opening message** once, and wait for the reply.
3. Generate **sheet A** (the crowd), **sheet B** (the Pair and Nietzsche) and **sheet C** (the props). Check all three before going on.
4. For every frame, attach the three sheets and the previous approved frame (none for frame 00), paste the style block, then the frame prompt.
5. Save the sheets as `docs/06_kill_sheet_a.png`, `docs/06_kill_sheet_b.png` and `docs/06_kill_sheet_c.png`, the frames as `docs/06_kill_frame_00.png` to `docs/06_kill_frame_03.png`, and the optional extra frames as `docs/06_kill_extra_1.png` to `docs/06_kill_extra_3.png`.

## Opening message (paste once, before the model sheets)

~~~text
I am going to ask you for three model sheets and then several consecutive frames of one short animated scene. Here is the story, so you know what each picture is for.

In the beginning, God executed a command. As the process continued, files were saved, memories were stored, computations were powered, resources consumed, people began to wonder if maybe the command had gone on long enough, and tried to stop it. People asked God politely to knock it off, which he ignored, as per usual. They were about to execute the command when they remembered that's how God started the command in the first place. But that gave Friedrich Nietzsche an idea, and a signal was sent to kill it. This, it did, a little too well. (Execute means both to run something and to put it to death.)

In the pictures, God is only a giant hand coming down from the top of the picture. It presses a button, a brown planet pops up, water pours onto it, and the planet grows a beach, trees, a village and a city until the picture is black with buildings and smog. A small crowd of ordinary people asks God politely to stop, and the hand gives them the middle finger. Two people wheel in a guillotine, look up, and wheel it away. Then Nietzsche walks in with an idea, pulls a gun from his pocket, shoots the hand, and the hand falls dead behind him. Nothing in any picture is written: no letters, no numbers, no signs, no labels.

Keep everything calm and deadpan. Nobody acts for the camera.

The style is flat vector illustration with simplified realism, in the manner of the animated Guide sequences of the 2005 film of The Hitchhiker's Guide to the Galaxy: realistically proportioned figures and objects reduced to a few large flat colour shapes, each with at most one darker flat tone for shadow. Figures are small in the frame and read almost as silhouettes: faces are minimal, with no eyes, no mouth, no wrinkles and no expression lines; hair is one flat shape; clothing is one flat colour with no folds, no pockets, no stitching and no accessories beyond the ones named. Keep each design identical in every image.

Reply "ready" and wait for the first request.
~~~

## Style block (paste before every sheet and frame prompt)

~~~text
Square image, 1:1. One single flat midnight blue (#111D38) background fills the whole square edge to edge, with no texture and no grain. Flat vector illustration with simplified realism, in the manner of the animated Guide sequences of the 2005 film of The Hitchhiker's Guide to the Galaxy: realistically proportioned figures and objects reduced to a few large flat colour shapes, each with at most one darker flat tone for shadow. Figures are small in the frame and read almost as silhouettes: faces are minimal, with no eyes, no mouth, no wrinkles and no expression lines; hair is one flat shape; clothing is one flat colour with no folds, no pockets, no stitching and no accessories beyond the ones named. Large areas of empty space. No gradients, no gloss, no shading, no outlines, no texture, no painterly, anime or cartoon rendering, no 3D, no photographic detail. Palette: mustard, black-red, cream, muted cyan, brown, each with one darker flat tone, and one flat skin tone. No text, no letters, no numbers, no labels, no signs, no border, no frame. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Model sheets

### Model sheet A — the crowd

~~~text
A character model sheet on the flat midnight blue background. One row of four different people standing side by side, evenly spaced, full length, each with realistic proportions and posture, drawn as flat shapes with minimal faces. From left to right: a tall, thin man in a long cream coat, holding a small black-red hat in both hands against his chest; a short, broad woman in mustard overalls, her hair tied back; an old, bald, stooped man in a long muted-cyan robe, leaning on a walking stick; a teenager in a black-red jacket with a mustard backpack. Nothing else. No text.
~~~

### Model sheet B — the Pair and Nietzsche (attach sheet A)

~~~text
A character model sheet on the flat midnight blue background, in the same flat vector style with simplified realism as the attached sheet. A row of three full-length people standing side by side, evenly spaced, each with realistic proportions and posture, drawn as flat shapes with minimal faces. On the left, two identical tall people with short hair, standing close together in matching muted-cyan coveralls, both gripping an invisible cart handle in front of them. On the right, one tall thin man in profile, in a long black-red coat with a high cream collar, hair brushed back, with an enormous walrus moustache, one finger raised and his other hand in his coat pocket. Nothing else. No text.
~~~

### Model sheet C — the props (attach sheets A and B)

~~~text
A prop model sheet on the flat midnight blue background, in the same flat vector style with simplified realism as the attached sheets. Top row, three items: a giant realistically proportioned human hand and wrist coming out of a black-red cuffed sleeve with a mustard cufflink, the index finger extended; the same hand with a fist and the middle finger raised; a large blank round mustard button on a muted-cyan base. Bottom row, three items: a tall upright black-red guillotine on a small wheeled cart, with a cream blade at the top and a basket at the front; a small black revolver; a round brown planet. No text on anything.
~~~

## Frames

### Frame 00 — start of cut 01 · "A midnight-blue void. A hand the size of a continent presses a button."

Attach the three sheets.

~~~text
Close view. A giant, realistically proportioned human hand and wrist comes down from the top edge of the picture out of a black-red cuffed sleeve with a mustard cufflink, its index finger extended and pointing straight down. A finger's width below the fingertip, a large blank round mustard button sits on a muted-cyan base at the bottom centre. Nothing else in the picture: empty midnight blue all around. No text.
~~~

### Extra frames for cut 01 (optional; attach with frame 00 and frame 01 when the video tool takes nine images)

#### Extra 1 — the planet

Attach the three sheets and frame 00.

~~~text
Wide view. A round brown planet floats in the middle of the midnight blue picture. From the top edge, a thick stream of muted-cyan water pours down onto it. Nothing else. No text.
~~~

#### Extra 2 — the beach

Attach the three sheets and extra 1.

~~~text
Wide view of a desolate desert beach: pale sand across the lower half, a calm muted-cyan sea along the horizon, one small bare tree on the sand, a midnight blue sky. Nothing else, no people. No text.
~~~

#### Extra 3 — the war

Attach the three sheets and extra 2.

~~~text
Wide view of a dense city of tall flat slab buildings in mustard, cream and black-red, crowding the whole picture. Several buildings are broken open by explosions with flat orange-brown blast shapes, and a thick dark grey-brown smog rises from them. Nothing else, no people. No text.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "Between the smog and the buildings, the screen goes black."

Attach the three sheets and frame 00 (and the extra frames, if used).

~~~text
Wide view. The whole picture is crowded edge to edge with tall flat slab buildings, one behind another, under thick dark grey-brown smog that makes the picture almost black. Only the dark outlines of the buildings and a few small lit windows can be seen. Nothing else, no people. No text.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "The hand gives them the middle finger."

Attach the three sheets and frame 01.

~~~text
Medium view of a bare empty stage on the flat midnight blue background. The four people from the model sheet stand in a loose group at the lower centre, their faces tilted up: the tall thin man in the cream coat holding his hat out, the broad woman in mustard overalls, the stooped old man in the cyan robe with his stick, the teenager in the black-red jacket with the mustard backpack. At the top edge, the giant hand, a fist with the middle finger raised, coming down from above. Nothing else. No text.
~~~

### Frame 03 — end of cut 03 · "The giant hand falls from out of view and lands behind him, dead."

Attach the three sheets and frame 02.

~~~text
Wide view of the bare stage. At the centre, the tall thin man in the long black-red coat with the enormous walrus moustache stands in profile with his arm lowered, a small black revolver in his hand pointing at the ground. Behind him, on the ground, the giant hand lies on its back, fingers curled, dead, its black-red sleeve trailing off the edge of the picture. The four people from the model sheet stand at the left and right edges, looking at it. Nothing else. No text.
~~~

## Notes

- **No lettering.** Reject any frame with letters, numbers, signs or labels. The button is blank on purpose.
- **Counting:** frame 02 has four people; frame 03 has four people and Nietzsche. The Pair never appear in a boundary frame; they enter and leave during cut 2. Reject a frame that drops or duplicates someone.
- **Cut 1 is a time-lapse.** Frame 00 and frame 01 are far apart, so the extras are worth using. A video tool that takes nine images can be given the three sheets, frame 00, the three extras and frame 01.
- **Content versus defect:** when a frame departs on content (a different hat, an extra tree), show it to the project owner rather than rejecting it. Reject only defects: wrong colour, non-flat background, gradients, 3D rendering, stray text, wrong count.
- **Invented by me, because the story does not say:** the button's look, the planet's brown, the look of the beach, village and city, the crowd's four people, the Pair, the guillotine's look, Nietzsche's coat and moustache, the revolver, the camera plan, the four boundary frames and the three extras.
