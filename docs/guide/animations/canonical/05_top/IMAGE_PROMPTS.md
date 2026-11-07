# 05_top — image prompts

Four boundary frames for the three cuts of `05_top` (15 seconds), plus two character model sheets. Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

This is the second design. The first one failed in three ways, all visible in the renders:

- **The ditch** read as a smile. With the tree and the rock sticking out of the hill's outline, the hill looked like a sleepy creature with antennae.
- **The characters** were six identical stick figures with no feet and no personality. Nothing about them could be animated or told apart.
- **The plan ignored motion.** Nothing said who moves where, so frames 02 and 03 came back as empty hills.

## The story, in plain words

Before `top`, King of the Hill had a great many winners. Each player stood wherever they happened to be on the hill and was king of it. Anyone who came near was pushed back down, and everyone knew the rules.

Trouble started when some players overheard others declaring themselves king, which was puzzling, since they had been king for some time. No player could see the whole hill, because of trees, boulders and the slope, so none of them could judge whether they were king. One player could see everyone, and everyone could see that player. When the players compared their lists of the kings they had seen, exactly one name was on every list. That was the top.

It did not tell anyone who had won. Until then, everyone had. The top invented losers.

## The hill, built so it can be played on

One locked camera, side view, for the whole 15 seconds. Only the people move. The hill is wide and low and fills the lower two thirds of the square, running off the bottom and both sides.

- **Two humps with a dip between them.** A taller main hump right of centre with a flat-ish summit, a smaller hump to its left, and a shallow dip (the saddle) between. A player in the dip cannot see over either hump. There is no ditch and no hole anywhere.
- **A big tree** with a thick trunk, planted in the middle of the main hump's left slope. Its canopy overlaps the hill, never the sky, so it cannot read as an antenna.
- **A big boulder**, half sunk into the main hump's right slope, low enough to stand on.
- Nothing sits on the hill's outline except the summit player.

## The cast: six different people

Everyone is a flat cream cut-paper figure built from separate simple shapes (round head with two dot eyes and no mouth, rounded body, two stubby arms, two stubby legs, small feet), so the pieces can bend and move. Each has a different silhouette, so the audience can follow who is who from a distance:

1. **Tall and thin**, with a tall top hat with a vermilion band. (A)
2. **Short and round-bodied**, with a bobble hat. (B)
3. **Medium**, with a long mustard scarf hanging down their back. (C)
4. **Medium, with wide shoulders**, and a flat cap. (D)
5. **Small, with an oversized round head** and a ponytail. (E)
6. **Medium, bald**, with a vermilion belt. (F)

Poses needed: standing with arms down, both arms up in a V (triumph), leaning forward with both arms straight out (shoving), tumbling sideways mid-roll with limbs out, and a head tilted with one hand cupped at an ear (listening).

## Where everyone is, in each frame

| Frame | Summit | Others |
|---|---|---|
| 01 | A, arms up | B behind the tree, C on the boulder, D in the dip, E at the left foot, F halfway up the right slope; all arms up |
| 02 | C, arms up (new king) | A tumbling down the right slope, D tumbling down the left slope, B beside the tree arms up, E and F listening, head tilted |
| 03 | C, arms up, crowned | A at the right foot, D on the left hump, B beside the tree, E at the left foot, F halfway up the right slope; all arms down, facing the summit |

## Motion plan, for the video generation

The camera never moves and the hill never changes. Only people move.

- **Cut 1 (0–5 s).** The empty hill. The six walk in from the left and right edges, one at a time about 0.7 s apart, each to their spot, then throw both arms up with a small hop. Hold the last second.
- **Cut 2 (5–10 s).** From 5 to 6 s the players nearest the summit turn toward it. B, C and D run toward A. A shoves each one back (both arms forward, lean). D reaches the summit first at about 7 s and A tumbles off down the right slope. C then shoves D at about 8.5 s; D tumbles down the left slope and C raises both arms. E and F stay put and tilt their heads, listening.
- **Cut 3 (10–15 s).** The tumblers get up and walk to a spot, arms down. Dashed lines of sight draw outward from each player to the others they can see, then every line also reaches C at the summit. A crown lands on C. The other five lower their arms. Hold on C for the last second.

## Colours

Background for the whole animation: dark teal `#073D43`, from this entry's generated prompts. The palette is mustard, mint green, vermilion and dusty lavender; the guide names these without hex values, so the hex values are my choices: mustard `#D9A12B`, mint green `#9FD8B8`, vermilion `#E34A2D`, dusty lavender `#A89CC8`. Cream `#F3E3B5` is added for the people, as in the cat tangent.

## How to use

1. Start one new ChatGPT conversation. Attach `docs/03_apt_get_update_frame_00.png` and say: "This is the style to match: flat matte cut-paper pictogram, one flat background colour. Do not copy its content."
2. Paste the **opening message** once, and wait for the reply.
3. Generate **model sheet A**, then **model sheet B**. Do not go on until both are right: six clearly different people, and four clear poses.
4. For every frame, attach both model sheets and the previous approved frame (none for frame 00), paste the style block, then the frame prompt.
5. Save approved sheets as `docs/05_top_sheet_a.png` and `docs/05_top_sheet_b.png`, and frames as `docs/05_top_frame_00.png` to `docs/05_top_frame_03.png`.

## Opening message (paste once, before the model sheets)

~~~text
I am going to ask you for two character model sheets and then four consecutive frames of one short animated scene. Here is the story, so you know what each picture is for.

It is the game King of the Hill, before anyone had invented a way to tell who had won. Six different players are scattered on a hill, each standing in a different place, each with both arms up, each believing they are king of the hill. Players run toward the summit and the king there shoves them back down. Sometimes a challenger gets through, and then the old king rolls down the hill and the challenger raises both arms. Most players cannot see the others, because of a tree, a boulder and the slope, so they cannot tell who has really won. One player, at the very top, can see everyone, and everyone can see that player. When everyone compares who they can see, one player is on every list. That player gets a crown. The other five lower their arms. The joke is that before this, everyone was a winner, and that the thing that found the winner invented losers.

Keep everything calm, flat and deadpan. Nobody acts for the camera.

The six players are different from each other, so a viewer can tell them apart. All are flat cream cut-paper figures with a round head with two dot eyes and no mouth, a rounded body, two stubby arms, two stubby legs and small feet. 1: tall and thin, tall top hat with a vermilion band. 2: short and round-bodied, bobble hat. 3: medium height, long mustard scarf hanging down the back. 4: medium height, wide shoulders, flat cap. 5: small, oversized round head, ponytail. 6: medium height, bald, vermilion belt. Keep each one identical in every image.

Reply "ready" and wait for the first request.
~~~

## Style block (paste before every sheet and frame prompt)

~~~text
Square image, 1:1. One single flat dark teal (#073D43) background fills the whole square edge to edge. Flat 2D screen-printed cut-paper pictogram style: simple opaque cutout shapes, economical geometric silhouettes, matte colour, slightly imperfect printed edges, large areas of empty space. No gradients, no gloss, no drop shadows, no 3D shading, no photographic detail. Only these colours besides the background: cream, mustard, mint green, vermilion, dusty lavender. No text, no letters, no numbers, no labels, no header, no border, no frame. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Model sheets

### Model sheet A — the six players

~~~text
A character model sheet on the flat dark teal background. Two rows of six small flat cream cut-paper people, evenly spaced, full body, front-facing. Top row: all six standing with arms down. Bottom row: the same six, each with both arms raised in a V. In each row, from left to right: 1 tall and thin with a tall top hat with a vermilion band; 2 short and round-bodied with a bobble hat; 3 medium height with a long mustard scarf hanging down the back; 4 medium height with wide shoulders and a flat cap; 5 small with an oversized round head and a ponytail; 6 medium height, bald, with a vermilion belt. Every person has a round head with two dot eyes and no mouth, a rounded body, two stubby arms, two stubby legs and small feet. Nothing else.
~~~

### Model sheet B — the poses (attach sheet A)

~~~text
A character model sheet on the flat dark teal background, using the same characters as the attached sheet, same designs exactly. Two rows of four. Top row: person 1 (tall and thin, top hat) in four poses, left to right: standing with arms down; both arms raised in a V; leaning forward with both arms straight out in front, shoving; tumbling sideways mid-roll with arms and legs out. Bottom row: person 2 (short and round, bobble hat) in the same four poses. Nothing else.
~~~

## Frames

All frames use this hill description, so repeat it exactly. **The hill:** one wide, low, flat dusty-lavender hill with two humps: a taller main hump right of centre with a flat-ish summit, and a smaller hump to its left, with a shallow dip between them. It fills the lower two thirds of the square and runs off the bottom and both sides. A large mint-green tree with a thick trunk and a broad round canopy stands in the middle of the main hump's left slope, with its canopy overlapping the hill. A large mint-green boulder is half sunk into the main hump's right slope. No ditch, no holes, no faces on anything. Each person stands about one seventh of the picture's height.

### Frame 00 — start of cut 01 · "A hill appears."

~~~text
The hill, exactly as described: one wide, low, flat dusty-lavender hill with two humps: a taller main hump right of centre with a flat-ish summit, and a smaller hump to its left, with a shallow dip between them, filling the lower two thirds of the square and running off the bottom and both sides. A large mint-green tree with a thick trunk and a broad round canopy stands in the middle of the main hump's left slope, canopy overlapping the hill. A large mint-green boulder is half sunk into the main hump's right slope. No ditch, no holes, no faces on anything. No people. Dark teal empty sky above.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "Six players are on it … Each has both arms in the air. Each is king."

Attach both model sheets and frame 00.

~~~text
The same hill, tree and boulder as the attached frame. The six players from the attached model sheets stand on it, each with both arms raised in a V, each in a different place: the tall thin one in the top hat at the very top of the main hump; the short round one with the bobble hat behind the tree trunk, half hidden; the one with the long mustard scarf standing on top of the boulder; the one with the flat cap standing down in the dip between the two humps; the small one with the ponytail at the far left foot of the hill; the bald one with the vermilion belt halfway up the main hump's right slope. All six are celebrating. Nothing else.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "Challengers run in … Twice, a challenger gets through. The king goes down the hill instead, and the challenger raises both arms."

Attach both model sheets and frame 01.

~~~text
The same hill, tree and boulder. At the summit, the person with the long mustard scarf stands with both arms raised, the new king. On the right slope, the tall thin person in the top hat tumbles down the hill, tilted sideways, mid-roll, limbs out. On the left slope, the person in the flat cap tumbles down the hill the same way. The short round person with the bobble hat stands beside the tree with both arms raised. The small person with the ponytail at the left foot and the bald person with the vermilion belt halfway up the right slope each tilt their head to one side with one hand cupped at an ear, listening, both arms otherwise relaxed. Nothing else.
~~~

### Frame 03 — end of cut 03 · "Thin lines of sight … A single crown settles on the summit player. The five others slowly lower their arms. Hold."

Attach both model sheets and frame 02.

~~~text
The same hill, tree and boulder. At the summit, the person with the long mustard scarf stands with both arms raised and a small mustard three-pointed crown on their head. The other five stand on the hill with both arms lowered at their sides, all facing the summit: the tall thin one in the top hat at the right foot of the hill, the one in the flat cap on the left hump, the short round one with the bobble hat beside the tree, the small one with the ponytail at the left foot, and the bald one with the vermilion belt halfway up the right slope. From each of the five, a thin dashed mustard line runs straight to the person at the summit. A few of the five also have one shorter dashed line to a neighbour. Nothing else.
~~~

## Notes

- **Check each sheet before using it.** Sheet A must show six clearly different silhouettes, and sheet B four clear poses. Everything after depends on them.
- **Counting:** the right number of people is six in frames 01, 02 and 03. Reject a frame that drops or duplicates someone.
- **Text:** there should be none. Reject letters, numbers or labels.
- **If frame 02 is crowded,** make it in two passes: first only the summit king with the two tumblers, then add the other three.
- **Invented by me, because the guide does not say:** the six designs, the humps and dip, the tree and boulder as the hiding places, who swaps with whom, cream for the people, the crown and the dashed lines as the picture of "lists". Reword freely.
- **Content versus defect:** when a frame departs on content (a different hat, an extra tree), show it to the project owner rather than rejecting it. Reject only defects: wrong colour, non-flat background, broken shapes, stray text, wrong count.
