# 05_top — image prompts

Four boundary frames for the three cuts of `05_top` (15 seconds). Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

Next in order after `04_nano`. This one is text-heavy, so read the notes at the bottom before you start.

## How to use

1. Start one new ChatGPT conversation. Attach `docs/03_apt_get_update_frame_00.png` and say: "This is the style to match: flat matte cut-paper pictogram, one flat background colour. Do not copy its content."
2. Paste the **style block**, then the prompt for frame 00. Generate until it is right.
3. For each later frame, attach the previous approved frame, paste the style block again, then the frame prompt. Say: "Keep the same three process characters, the same leaderboard layout and the same lettering style as the previous frame."
4. Save approved frames as `docs/05_top_frame_00.png` to `docs/05_top_frame_03.png`. The Seedance pack builder (`scripts/build_seedance_packs.py`) picks them up by that name.

## Colours

Background for the whole animation: dark teal `#073D43`, taken from this entry's generated prompts. Palette: mustard `#D9A12B`, mint green `#9FD8B8`, vermilion `#E34A2D`, dusty lavender `#A89CC8`. The guide names these colours but gives no hex for them; the hex values are my choices.

## Style block (paste before every frame prompt)

~~~text
Square image, 1:1. One single flat dark teal (#073D43) background fills the whole square edge to edge. Flat 2D screen-printed cut-paper pictogram style: simple opaque cutout shapes, economical geometric silhouettes, matte colour, slightly imperfect printed edges, large areas of empty space. No gradients, no gloss, no drop shadows, no 3D shading, no photographic detail. Only these colours besides the background: mustard, mint green, vermilion, dusty lavender. No header, no tabs, no border, no frame, no captions. The only lettering allowed is the text the frame prompt gives in quotation marks, in a heavy geometric sans-serif, spelled exactly as written, and nothing else. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Frames

### Frame 00 — start of cut 01 · "A flat process leaderboard appears under the header `SORT: %CPU`."

~~~text
A flat leaderboard fills the middle of the square. Across its top, a mustard strip with the text "SORT: %CPU" in dark teal. Under it, three rows stacked evenly, each row a long rounded mint-green bar. Each row has, at its left, a small simple round-bodied process character in a different colour, and the row's text at its right, in dark teal: row one "sensible_process 2%" with a mint-green character standing upright; row two "useful_process 1%" with a dusty-lavender character standing upright; row three "idle_process 0%" with a mustard character slumped and half asleep. Each row also has a thin percentage bar that is almost empty. Nothing else in the picture; a lot of empty teal around the board.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "A large clipboard labeled `PERFORMANCE REVIEW` slides in from frame right. The bottom process notices it. End on the bottom process staring at the clipboard."

~~~text
The same leaderboard and the same three process characters, unchanged: "SORT: %CPU", "sensible_process 2%", "useful_process 1%", "idle_process 0%". A large flat vermilion clipboard with a cream-coloured sheet has slid in from the right and stands at the right of the square, partly overlapping the board's edge, with the text "PERFORMANCE REVIEW" in dark teal across its top. In the bottom row, the mustard idle_process character is now sitting upright, wide awake, with its head turned toward the clipboard, staring at it. The two rows above carry on as before.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "The bottom process begins literally shovelling little CPU blocks into itself; its percentage climbs and its row moves upward. The other processes notice and imitate it. … End with all rows racing upward in CPU use."

~~~text
The same board, the same three characters and the same clipboard at the right. Every process character is now shovelling small vermilion square blocks into its own body with a little flat shovel. The rows have reordered and the percentage bars are filling: first row, the mustard character, "idle_process 81%"; second row, the dusty-lavender character, "useful_process 63%"; third row, the mint-green character, "sensible_process 47%". The characters lean forward, frantic but still plain flat shapes. A few loose vermilion blocks fly between the rows. The header still reads "SORT: %CPU".
~~~

### Frame 03 — end of cut 03 · "One process hits `99%` and receives a tiny trophy. Confetti appears for half a second; simultaneously the entire terminal freezes rigid. A boardroom stamp descends … `RECORD QUARTER`. Hold on the trophy beside a completely unusable system."

~~~text
The same board, frozen rigid. The top row reads "idle_process 99%", the mustard character standing very upright beside a tiny mustard trophy cup. The two rows below are stuck mid-motion with their shovels and blocks hanging unmoving in the air. Flat confetti pieces in mint green, mustard and dusty lavender hang motionless across the picture. Over the whole picture, tilted slightly, a large vermilion rectangular boardroom stamp mark with the text "RECORD QUARTER" in cream, as if pressed down onto the frozen scene. The header still reads "SORT: %CPU".
~~~

## Notes

- Text is the risk, and this scene has the most of it. Required, exactly as written: "SORT: %CPU", "sensible_process", "useful_process", "idle_process", their percentages, "PERFORMANCE REVIEW", "99%", "RECORD QUARTER". Reject any frame with a misspelling or an extra word. If a text item will not come out right after three tries, stop and ask the project owner; the guide's own advice is to leave a clean area and composite exact text in post.
- The percentages in frame 02 (81%, 63%, 47%) and the row order are my choices; the beat only says "all rows racing upward". Reword freely.
- Frame 03 shows frozen by suspending the confetti and shovels mid-air under the stamp. The beat gives no further look for "frozen"; if ChatGPT adds a frost or glitch effect, show it to the project owner rather than rejecting it.
- Which process reaches 99% is not specified ("one process"). I chose the bottom one, since it starts the run.
- When a frame departs from a prompt on content (an extra prop, a different layout), show it to the project owner rather than rejecting it; reject only defects (wrong colour, non-flat background, broken shape, stray text).
