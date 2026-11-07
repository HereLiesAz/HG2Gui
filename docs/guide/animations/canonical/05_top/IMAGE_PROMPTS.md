# 05_top — image prompts

Four boundary frames for the three cuts of `05_top` (15 seconds), plus two character model sheets. Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

This is the third design. The first two failed because they did not look like the Guide's own animations. The cast and style below are taken from the project's existing frames, in `docs/guide/project/references/shared/` (the `echo_clip_*`, `ls_clip_*` and `ls_tangent_clip_*` frames).

## What the existing Guide frames teach

- **People are tall, elegant paper-cut figures, not blobs.** Real proportions, drawn in side or three-quarter view, with a simple profile face (a small nose, a bit of hair), a tunic, a belt, trousers, boots. Hands and boots are picked out in a second colour.
- **A recurring cast.** Uniformed functionaries in cap, tunic and boots. A woman in a jumpsuit with a bob haircut and a red megaphone. A big green bear-headed creature in a blue coat, with a cane, who is always the heaviest figure in the room.
- **Diagram language.** Sound-wave arcs growing outward from a horn. A dashed arrow with an arrowhead. Copies of a figure fading into the distance. Meaning comes from these, not from lettering.
- **Plain staging.** Two to four things per frame, large, on one flat colour field, with a little paper grain. A few heavy props: a green filing cabinet, a table, a plant. Deadpan poses.

## The story, in plain words

Before `top`, King of the Hill had a great many winners. Each player stood wherever they happened to be on the hill and was king of it. Anyone who came near was pushed back down, and everyone knew the rules.

Trouble started when some players overheard others declaring themselves king, which was puzzling, since they had been king for some time. No player could see the whole hill, because of trees, boulders and the slope, so none of them could judge whether they were king. One player could see everyone, and everyone could see that player. When the players compared their lists of the kings they had seen, exactly one name was on every list. That was the top.

It did not tell anyone who had won. Until then, everyone had. The top invented losers.

## The cast: six Guide characters

Costumes use the entry palette (mustard, dark teal, mint green, vermilion, dusty lavender) plus cream for the pale figure.

1. **The Bear.** The big bear-headed creature: mint-green head, dusty-lavender coat, a cane. By far the biggest figure. This is who ends up on top, and the reason everyone can see him: he is simply too big to hide. Nobody can push him.
2. **The Megaphone Woman.** Slim, vermilion jumpsuit, bob haircut, a mustard megaphone. She declares herself king through it.
3. **The Cap Man.** Vermilion tunic and trousers, flat mustard cap, mustard boots, a belt.
4. **The Peaked-Cap Man.** The tallest of the people. Vermilion tunic, a tall red peaked cap, hands on his hips.
5. **The Pale Man.** Cream shirt and trousers, bare-headed, mustard boots.
6. **The Bowler Man.** Short and stout, a mustard coat, a dark-teal bowler hat and a mustard scarf.

Poses needed: triumph (both arms up), shoving (leaning in, both arms forward), tumbling (rolling down a slope, limbs out), listening (head tilted, a hand cupped to an ear), and standing with arms down.

## The stage: a round hill, seen by a camera that circles it

The camera **must move**. It orbits the hill the whole 15 seconds, once all the way round. Because the hill is solid, the camera, like the players, can never see all six at once: whoever is on the far side is hidden behind the hill. That is the players' problem, shown by the camera. The one thing visible from every angle is whoever stands on the summit.

The hill is a wide, low, round mound, flat and plain like a paper cake. The look is a paper-theatre diorama on a turntable: flat shapes, simple overlaps, and the camera sliding round. It must not turn into a 3D render.

Features, by compass position round the hill (0° is the front at the start, angles run clockwise seen from above):

- **Summit** (centre): flat-ish top, visible from every side.
- **A big tree** at 60°: thick trunk, broad canopy overlapping the hill, never the sky.
- **A big boulder** at 120°, half sunk into the slope, low enough to stand on.
- **A small second bump** at 180° with a shallow dip between it and the main mound. A player in the dip cannot see over either.
- Hill colour: mustard, on the dark teal background. Tree canopy mint green, boulder dusty lavender.
- Nothing sits on the hill's outline except the summit figure.

A camera at angle θ sees a feature at angle φ in front when φ is within 90° of θ: at the centre when φ = θ, at the right edge when φ is 90° clockwise of θ, at the left edge when 90° anticlockwise. Anything further round is hidden behind the hill.

## Camera plan

- **Cut 1 (0–5 s):** the camera orbits from 0° to 120°.
- **Cut 2 (5–10 s):** 120° to 240°, closing in a little on the summit.
- **Cut 3 (10–15 s):** 240° to 360°, back to where it began, easing out to the wide view.
- Speed is slow and even. The summit stays at the centre of the picture the whole way.

## Where everyone is

The six players have compass positions at the start: the Pale Man in the dip at 180°, the Bear halfway up at 300°, the Peaked-Cap Man on the boulder at 120°, the Cap Man behind the tree at 60°, the Bowler Man at the foot at 0°, and the Megaphone Woman at the summit.

| Frame | Camera | What is in view |
|---|---|---|
| 00 | 0° | The empty hill: the tree at the right edge, the foot in front. No one on it. |
| 01 | 120° | Tree at the left with the Cap Man behind the trunk; the boulder in the centre front with the Peaked-Cap Man on it; the small bump at the right with the Pale Man in the dip; the Megaphone Woman at the summit. All arms up. The Bowler Man and the Bear are hidden behind the hill. |
| 02 | 240° | Medium view of the upper hill. The Bear stands at the summit with both arms and his cane up. The Pale Man tumbles off to the left, the Megaphone Woman off to the right, her megaphone flying. The Peaked-Cap Man and the Cap Man scramble up the slope below, pushed back. The rest of the hill is hidden. |
| 03 | 360° | The same view as frame 00. The Bear at the summit, crowned. The Bowler Man at the foot, the Cap Man at the tree and the Megaphone Woman at the left foot, arms down, looking up. Dashed cream arrows from each of the three to the Bear. Two more dashed arrows rise from behind the hill's outline to the Bear: from the Peaked-Cap Man and the Pale Man, whom we cannot see. |

## Motion plan, for the video generation

The camera never stops moving. Only the people, and the camera, move.

- **Cut 1 (0–5 s).** The empty hill. As the camera starts round, the players walk in one at a time, each to their spot, and raise both arms. The camera passes the Bowler Man, the Cap Man, the Peaked-Cap Man and the Pale Man, and at the summit the Megaphone Woman raises her megaphone, sound-wave arcs rippling out over the hill. The camera never sees the Bear or, at the end, the Bowler Man.
- **Cut 2 (5–10 s).** The camera keeps circling and nudges in. The Pale Man, Peaked-Cap Man and Cap Man run for the summit and the Woman shoves each one back. At about 7 s the Pale Man gets through and she tumbles off. Then the Bear lumbers up from the far side, arrives in view at the summit, and the Pale Man cannot stop him. He tumbles off the other side. The Bear raises both arms and his cane. Players the camera cannot see tilt their heads, listening.
- **Cut 3 (10–15 s).** The camera completes the circle. The tumblers get up and walk to a spot, arms down. Dashed cream arrows draw outward from each visible player to the Bear, and two more rise from behind the hill. A crown lands on the Bear. The other five lower their arms. Hold on the Bear for the last second as the camera settles.

## Colours

Background for the whole animation: dark teal `#073D43`, from this entry's generated prompts. The palette is mustard, mint green, vermilion and dusty lavender; the guide names these without hex values, so the hex values are my choices: mustard `#D9A12B`, mint green `#9FD8B8`, vermilion `#E34A2D`, dusty lavender `#A89CC8`. Cream `#F3E3B5` is for the Pale Man and the dashed arrows.

## How to use

1. Start one new ChatGPT conversation. Attach the three style references from `docs/guide/project/references/shared/`: `echo_clip_01_start.png` (the megaphone woman), `echo_clip_03_start.png` (the bear and the men) and `ls_clip_01_start.png` (the man at the door). Say: "These are the Guide's own animation frames. Match this exact style: flat paper-cut figures with real proportions, simple profile faces, uniforms and boots, a flat colour field, a little paper grain. Do not copy their content."
2. Paste the **opening message** once, and wait for the reply.
3. Generate **model sheet A**, then **model sheet B**. Do not go on until both are right.
4. For every frame, attach both model sheets and the previous approved frame (none for frame 00), paste the style block, then the frame prompt.
5. Save approved sheets as `docs/05_top_sheet_a.png` and `docs/05_top_sheet_b.png`, and frames as `docs/05_top_frame_00.png` to `docs/05_top_frame_03.png`.

## Opening message (paste once, before the model sheets)

~~~text
I am going to ask you for two character model sheets and then four consecutive frames of one short animated scene. Here is the story, so you know what each picture is for.

It is the game King of the Hill, before anyone had invented a way to tell who had won. Six characters are scattered on a hill, each standing in a different place, each with both arms up, each believing they are king. They run toward the summit and the king there shoves them back down. Sometimes a challenger gets through, and the old king rolls down the hill. Most of them cannot see the others, because of a tree, a boulder and the slope, so they cannot tell who has really won, and some overhear others declaring themselves king through a megaphone. One character, a huge bear, ends up at the very top, and everyone can see him because he is so big. When everyone compares who they can see, he is on every list. He gets a crown. The others lower their arms. The joke is that before this, everyone was a winner, and that the thing that found the winner invented losers.

The camera circles the hill the whole time, so in any single picture some of the six are hidden behind the hill. Never try to fit all six into one picture; I will tell you who is in view.

Keep everything calm, flat and deadpan. Nobody acts for the camera.

The six characters are in the style of the attached Guide frames: flat paper-cut figures with real proportions and simple profile faces. 1: a huge bear-headed creature with a mint-green head, a dusty-lavender coat and a cane, far bigger than the rest. 2: a slim woman with a bob haircut in a vermilion jumpsuit with a mustard megaphone. 3: a man in a vermilion tunic and trousers, a flat mustard cap, mustard boots and a belt. 4: the tallest of the people, a man in a vermilion tunic with a tall red peaked cap, hands on his hips. 5: a bare-headed man in a cream shirt and trousers with mustard boots. 6: a short stout man in a mustard coat, a dark-teal bowler hat and a mustard scarf. Keep each one identical in every image.

Reply "ready" and wait for the first request.
~~~

## Style block (paste before every sheet and frame prompt)

~~~text
Square image, 1:1. One single flat dark teal (#073D43) background fills the whole square edge to edge, with a very light paper grain. Flat 2D paper-cut illustration in the style of the attached Guide frames: tall elegant figures with real proportions, simple profile faces, solid flat colour shapes, slightly imperfect printed edges, large areas of empty space. No gradients, no gloss, no drop shadows, no 3D shading, no photographic detail. Only these colours besides the background: cream, mustard, mint green, vermilion, dusty lavender. No text, no letters, no numbers, no labels, no header, no border, no frame. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Model sheets

### Model sheet A — the six characters

~~~text
A character model sheet on the flat dark teal background. Six full-length characters in one row, standing side by side, facing slightly toward the right, evenly spaced, in the style of the attached Guide frames. From left to right: a huge bear-headed creature with a mint-green head, a dusty-lavender coat and a cane, drawn about one and a half times the height of the others; a slim woman with a bob haircut in a vermilion jumpsuit holding a mustard megaphone; a man in a vermilion tunic and trousers with a flat mustard cap, mustard boots and a belt; the tallest man, in a vermilion tunic with a tall red peaked cap, hands on his hips; a bare-headed man in a cream shirt and trousers with mustard boots; a short stout man in a mustard coat with a dark-teal bowler hat and a mustard scarf. Nothing else.
~~~

### Model sheet B — the poses (attach sheet A)

~~~text
A character model sheet on the flat dark teal background, using the same characters as the attached sheet, same designs exactly. Two rows of four. Top row: the megaphone woman in four poses, left to right: standing with the megaphone raised to her mouth and one hand on her hip; both arms raised in triumph; leaning forward with both arms straight out, shoving; tumbling sideways mid-roll with her arms and legs out. Bottom row: the huge bear-headed creature in four poses: standing with arms down and the cane planted; both arms and the cane raised in triumph; leaning forward with both arms out, shoving; head tilted with one hand cupped to his ear, listening. Nothing else.
~~~

## Frames

**The hill, repeated in every frame:** a wide, low, round, flat mustard mound, like a paper cake, with a tree with a thick trunk and a broad mint-green canopy that overlaps the hill, a dusty-lavender boulder half sunk into the slope, and a small second bump with a shallow dip beside the main mound. No ditch, no holes, no faces on anything. The camera is level with the lower slopes.

### Frame 00 — start of cut 01 · "A hill appears." (camera 0°)

~~~text
Wide view. One wide, low, round, flat mustard hill, like a paper cake, fills the lower two thirds of the square and runs off the bottom and both sides. A large mint-green tree with a thick trunk and a broad round canopy stands at the right edge of the hill, with its canopy overlapping the hill. No boulder and no second bump are in view. No ditch, no holes, no faces on anything. No people. Dark teal empty sky above.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "Six players are on it … Each has both arms in the air. Each is king." (camera 120°)

Attach both model sheets and frame 00.

~~~text
Wide view of the same round mustard hill, now seen from further round, as if the camera had circled a third of the way. At the left, the large mint-green tree, and the man in the flat mustard cap standing behind its trunk, half hidden, both arms up. In the centre front, the dusty-lavender boulder half sunk into the slope, and the tallest man in the red peaked cap standing on top of it with both arms up. At the right, a small second bump with a shallow dip beside it, and the bare-headed man in cream standing in the dip with both arms up. At the very top of the hill, the slim woman in the vermilion jumpsuit holding her mustard megaphone up to her mouth, one arm raised. All four are celebrating. Nobody else is in view. Nothing else.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "Challengers run in … the challenger raises both arms." (camera 240°)

Attach both model sheets and frame 01.

~~~text
Medium view of the upper half of the same hill, seen from the far side, as if the camera had circled two thirds of the way round. The huge bear-headed creature stands on the flat summit in the middle with both arms and his cane raised in triumph. At the left, the bare-headed man in cream tumbles off the hill, tilted sideways, mid-roll, limbs out. At the right, the slim woman in the vermilion jumpsuit tumbles off the other way, mid-roll, her mustard megaphone flying through the air above her. Lower on the near slope, the tallest man in the peaked cap and the man in the flat cap scramble up with their arms out, pushed back. The tree and the boulder are not in view. Nothing else.
~~~

### Frame 03 — end of cut 03 · "Thin lines of sight … A single crown settles on the summit player. The five others slowly lower their arms. Hold." (camera 360°)

Attach both model sheets and frame 02.

~~~text
Wide view, the same as frame 00 but now with people: the camera has come all the way round. The large mint-green tree at the right edge with the man in the flat cap beside it, both arms down. At the front foot of the hill, the short stout man in the bowler hat, both arms down. At the left foot, the slim woman in the vermilion jumpsuit, both arms down. All three face the summit. At the summit, the huge bear-headed creature stands with both arms and his cane raised and a small mustard three-pointed crown on his head. From each of the three, a thin dashed cream line with a small arrowhead runs straight to the bear. Two more dashed cream lines with arrowheads rise from behind the outline of the hill to the bear, from people we cannot see. Nothing else.
~~~

## Notes

- **Check each sheet before using it.** Sheet A must show six clearly different people, the Bear far bigger than the rest, and all of them in the Guide's flat paper-cut style with real proportions. Sheet B must show four clear poses for each of the two characters.
- **Counting:** reject a frame that drops or duplicates a character. The camera hides some people on purpose, so frame 01 shows four, frame 02 shows five (the Bear, the two tumblers and the two scramblers) and frame 03 shows four (the Bear and three others) plus two arrows from hidden players. Do not ask for all six to be visible.
- **Text:** none at all. The megaphone's sound arcs and the dashed arrows are marks, not letters. Reject any letters or numbers.
- **If frame 02 is crowded,** make it in two passes: first the Bear at the summit with the two tumblers, then add the two scramblers.
- **The orbit is a camera move, so do not ask ChatGPT for a 3D scene.** Each frame is a flat paper-cut picture of what the camera sees at that angle.
- **Invented by me, because the guide does not say:** the six designs, the Bear as the king and the reason he can be seen, the Woman's megaphone, who swaps with whom, the compass positions and what each camera angle hides, cream for the dashed arrows, the crown. The camera orbit is the project owner's. The cast and style come from the project's existing frames. Reword freely.
- **Content versus defect:** when a frame departs on content (a different hat, an extra bush), show it to the project owner rather than rejecting it. Reject only defects: wrong colour, non-flat background, broken shapes, stray text, wrong count.
