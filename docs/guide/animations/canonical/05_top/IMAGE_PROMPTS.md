# 05_top — image prompts

Four boundary frames for the three cuts of `05_top` (15 seconds). Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

This replaces the earlier performance-review version of the scene, which is gone from the repo.

## The story, in plain words

Before `top`, King of the Hill had a great many winners. Each player stood wherever they happened to be on the hill and was king of it. Anyone who came near was pushed back down, and everyone knew the rules.

Trouble started when some players overheard others declaring themselves king, which was puzzling, since they had been king for some time. No player could see the whole hill, because of trees, ditches and the slope, so none of them could judge whether they were king. One player could see everyone, and everyone could see that player. When the players compared their lists of the kings they had seen, exactly one name was on every list. That was the top.

It did not tell anyone who had won. Until then, everyone had. The top invented losers.

## What the pictures need to show

Four pictures, each one calm and simple. There is no lettering anywhere, and no numbers.

- **Frame 00:** the empty hill.
- **Frame 01:** six kings, scattered, arms up.
- **Frame 02:** the fight for the summit; the king there has just been replaced.
- **Frame 03:** the lines of sight, the crown, one king left, five players with their arms down.

## Cast and set

All six players are the same small figure: a vermilion bean-shaped body, a round head, no face, two thin arms. Arms up is a V shape. Identical figures are the point of the joke, and they are far easier for an image generator to keep consistent.

- The hill: one smooth dusty-lavender mound.
- A tree, left of centre on the slope: a mint-green lollipop tree.
- A rock, right of centre on the slope: a mint-green rounded rock.
- A ditch, low on the slope: a dark-teal notch cut into the hill.
- The crown: mustard, plain, three points.
- The lines of sight: thin dashed mustard lines.

## Colours

Background for the whole animation: dark teal `#073D43`, from this entry's generated prompts. The palette is mustard, mint green, vermilion and dusty lavender; the guide names these without hex values, so the hex values below are my choices: mustard `#D9A12B`, mint green `#9FD8B8`, vermilion `#E34A2D`, dusty lavender `#A89CC8`.

## How to use

1. Start one new ChatGPT conversation. Attach `docs/03_apt_get_update_frame_00.png` and say: "This is the style to match: flat matte cut-paper pictogram, one flat background colour. Do not copy its content."
2. Paste the **opening message** once, and wait for the reply.
3. Paste the **style block**, then the prompt for frame 00. Regenerate until it is right.
4. For each later frame, attach the previous approved frame, paste the style block, then the frame prompt, and say: "Keep the same hill, the same props and the same figures as the previous frame."
5. Save approved frames as `docs/05_top_frame_00.png` to `docs/05_top_frame_03.png`.

## Opening message (paste once, before frame 00)

~~~text
I am going to ask you for four images that are consecutive frames of one short animated scene. Here is the story, so you know what each picture is for.

It is the game King of the Hill, before anyone had invented a way to tell who had won. Six players are scattered on a hill, each standing in a different place, each with both arms up, each believing they are king of the hill. Challengers run in and the players push them off. Sometimes a challenger gets through, and then the old king rolls down the hill and the challenger raises both arms. Most players cannot see the others, because of a tree, a ditch and the slope, so they cannot tell who has really won. One player, at the very top, can see everyone, and everyone can see that player. When everyone compares who they can see, one player is on every list. That player gets a crown. The other five lower their arms. The joke is that before this, everyone was a winner, and that the thing that found the winner invented losers.

Keep everything calm, flat and deadpan. Nobody acts for the camera. The six players are identical small figures: a vermilion bean-shaped body, a round head with no face, two thin arms. Arms up is a V shape. Keep them identical in every image.

Reply "ready" and wait for the first image request.
~~~

## Style block (paste before every frame prompt)

~~~text
Square image, 1:1. One single flat dark teal (#073D43) background fills the whole square edge to edge. Flat 2D screen-printed cut-paper pictogram style: simple opaque cutout shapes, economical geometric silhouettes, matte colour, slightly imperfect printed edges, large areas of empty space. No gradients, no gloss, no drop shadows, no 3D shading, no photographic detail. Only these colours besides the background: mustard, mint green, vermilion, dusty lavender. No text, no letters, no numbers, no labels, no header, no border, no frame. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Frames

### Frame 00 — start of cut 01 · "A hill appears."

~~~text
One smooth dusty-lavender hill, a wide rounded mound, sits low in the square with dark teal sky above it. On its left slope, one small mint-green lollipop tree. On its right slope, one mint-green rounded rock. Low on the slope, one dark-teal notch cut into the hill like a ditch. Nothing else. No people.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "Six players are on it … Each has both arms in the air. Each is king."

~~~text
The same hill, tree, rock and ditch. Six identical small vermilion players, bean-shaped body and round faceless head, each with both thin arms raised in a V. They stand scattered: one at the very foot of the hill at the left, one in the ditch so that only its head and raised arms show above the notch, one half hidden behind the tree, one halfway up the right slope, one on top of the rock, and one at the summit. Every one of them is celebrating. Nothing else.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "Challengers run in … The king there pushes each one back down the hill. Twice, a challenger gets through. The king goes down the hill instead, and the challenger raises both arms."

~~~text
The same hill and props. At the summit, one vermilion player stands with both arms raised, newly crowned king of the spot. Just below the summit on the slope, the previous king tumbles head over heels down the hill, tilted sideways, mid-roll, with a few small mustard motion marks behind it. Lower on the slope, two more vermilion figures are rolling down, also tilted, pushed off. The other four players are exactly where they were in the previous frame, still with both arms raised. A few thin mustard curved lines like sound waves reach out from the summit player toward the others. Nothing else.
~~~

### Frame 03 — end of cut 03 · "Thin lines of sight … A single crown settles on the summit player. The five others slowly lower their arms. Hold."

~~~text
The same hill and props, with all six players back in their places from frame 01. The player at the summit stands with both arms raised and a small mustard three-pointed crown on its head. The other five players have both arms lowered at their sides, facing the summit. From each of the five, a thin dashed mustard line runs straight to the summit player. A few of the five also have one shorter dashed line to a neighbour. Nothing else.
~~~

## Notes

- **Too busy:** frame 02 is the hardest picture. If ChatGPT crowds it, generate it in two passes: first the summit swap alone (king rolling down, new king up, nothing else), then ask it to add the two extra tumbling figures.
- **Counting:** reject any frame with the wrong number of players. The count is six in frames 01 and 03, and a mix of standing and tumbling in frame 02.
- **Text:** there should be none at all. Reject any frame with letters, numbers or labels.
- **Invented by me, because the guide does not say:** the identical vermilion figures, the six players, the exact spots, the mint props, the crown and the dashed lines as a picture of "lists", and that the final king's arms stay up. Reword freely.
- **Content versus defect:** when a frame departs from the prompt on content (an extra tree, a different crown), show it to the project owner rather than rejecting it. Reject only defects: wrong colour, non-flat background, broken shapes, stray text, wrong player count.
