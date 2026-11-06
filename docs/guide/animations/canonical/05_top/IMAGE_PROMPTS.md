# 05_top — image prompts

Four boundary frames for the three cuts of `05_top` (15 seconds). Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

Next in order after `04_nano`. This one is text-heavy, so read the notes at the bottom before you start.

## The story, in plain words

`top` is the Unix command that lists the programs running on a computer ("processes"), ranked by how much of the processor (CPU) each one is using. In this film the processes are office workers at a company, and the leaderboard is their quarterly ranking.

There are three. `sensible_process` is a tidy worker who does exactly what is needed (2%). `useful_process` does a little (1%). `idle_process` does nothing at all (0%) and is asleep at the bottom of the board.

A performance review arrives, as a large clipboard. The bottom worker wakes up, stares at it, and works out the only thing the review measures is the CPU number, not whether any work gets done. So it starts literally shovelling CPU into itself to climb the board. The other two notice and copy it. Soon all three are frantically stuffing themselves, racing upward. The bottom worker reaches 99% and is handed a tiny trophy. At that moment the computer, with no CPU left for anything else, freezes solid. The Board of directors stamps the result RECORD QUARTER.

The joke is deadpan and one idea long: everyone is rewarded for a number going up, and the machine is destroyed by it. Nobody performs for the camera; the staging stays calm, flat and diagrammatic. (This reading of the scene is mine. The guide gives the beats, not the reason; the clipboard is the cause I have chosen.)

## Cast

All three workers share one body: a small round-bodied, flat silhouette with two short legs, two stubby arms and a round head with two dot eyes. They differ only in colour and one marker item, so they stay recognisable in every frame.

- `sensible_process`: mint green, stands upright, a tiny necktie.
- `useful_process`: dusty lavender, stands upright, a small wrench tucked under one arm.
- `idle_process`: mustard, slouched, a tiny nightcap and a small pillow hugged to its chest.

Props: the leaderboard (a mustard header strip over three rows), the clipboard (vermilion, with a cream sheet), little vermilion CPU blocks, a flat shovel for each worker, a tiny mustard trophy cup, and a vermilion boardroom stamp.

## How to use

1. Start one new ChatGPT conversation. Attach `docs/03_apt_get_update_frame_00.png` and say: "This is the style to match: flat matte cut-paper pictogram, one flat background colour. Do not copy its content."
2. Paste the **opening message** below, once. It tells ChatGPT the story and the cast. Wait for it to answer.
3. Paste the **style block**, then the prompt for frame 00. Generate until it is right.
4. For each later frame, attach the previous approved frame, paste the style block again, then the frame prompt. Say: "Keep the same three workers, the same leaderboard layout and the same lettering style as the previous frame."
5. Save approved frames as `docs/05_top_frame_00.png` to `docs/05_top_frame_03.png`. The Seedance pack builder (`scripts/build_seedance_packs.py`) picks them up by that name.

## Opening message (paste once, before frame 00)

~~~text
I am going to ask you for four images that are consecutive frames of one short animated scene. Here is what the scene is about, so you understand what each picture is for.

"top" is the command that lists a computer's running programs, ranked by how much processor time (CPU) each uses. In our film those programs are office workers, and the ranking board is their quarterly leaderboard. There are three workers. sensible_process is a tidy worker who does just what is needed and uses 2%. useful_process does a little and uses 1%. idle_process does nothing and uses 0%; it is asleep at the bottom of the board.

A performance review arrives as a big clipboard. idle_process wakes up, stares at it, and realises the review only measures the CPU number, so it starts literally shovelling little CPU blocks into itself to climb the board. The other two notice and copy it, until all three are frantically stuffing themselves and racing upward. idle_process reaches 99% and is handed a tiny trophy. At that moment the whole computer freezes solid, because there is no CPU left for anything else. The company's Board stamps the result RECORD QUARTER.

The joke is deadpan: everyone is rewarded for a number going up, and the machine is destroyed by it. Nobody acts for the camera. Keep everything calm, flat and diagrammatic.

The three workers share one body: a small round-bodied flat silhouette with two short legs, two stubby arms and a round head with two dot eyes. They differ only in colour and one marker item. sensible_process is mint green, upright, with a tiny necktie. useful_process is dusty lavender, upright, with a small wrench under one arm. idle_process is mustard, slouched, with a tiny nightcap and a small pillow hugged to its chest. Keep them identical in every image.

Reply "ready" and then wait for the first image request.
~~~

## Colours

Background for the whole animation: dark teal `#073D43`, taken from this entry's generated prompts. Palette: mustard `#D9A12B`, mint green `#9FD8B8`, vermilion `#E34A2D`, dusty lavender `#A89CC8`. The guide names these colours but gives no hex for them; the hex values are my choices.

## Style block (paste before every frame prompt)

~~~text
Square image, 1:1. One single flat dark teal (#073D43) background fills the whole square edge to edge. Flat 2D screen-printed cut-paper pictogram style: simple opaque cutout shapes, economical geometric silhouettes, matte colour, slightly imperfect printed edges, large areas of empty space. No gradients, no gloss, no drop shadows, no 3D shading, no photographic detail. Only these colours besides the background: mustard, mint green, vermilion, dusty lavender. No header, no tabs, no border, no frame, no captions. The only lettering allowed is the text the frame prompt gives in quotation marks, in a heavy geometric sans-serif, spelled exactly as written, and nothing else. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Frames

### Frame 00 — start of cut 01 · "A flat process leaderboard appears under the header `SORT: %CPU`."

~~~text
The company's quarterly leaderboard fills the middle of the square. Across its top, a mustard strip with the text "SORT: %CPU" in dark teal. Under it, three rows stacked evenly, each a long rounded mint-green bar. In each row, the worker stands at the left and the row's text sits at the right in dark teal. Row one: the mint-green necktie worker standing neatly, "sensible_process 2%". Row two: the dusty-lavender worker with the wrench, standing neatly, "useful_process 1%". Row three: the mustard worker in the nightcap, slumped over its pillow, fast asleep, "idle_process 0%". Each row also has a thin percentage bar that is almost empty. Nothing else in the picture; a lot of empty teal around the board.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "A large clipboard labeled `PERFORMANCE REVIEW` slides in from frame right. The bottom process notices it. End on the bottom process staring at the clipboard."

~~~text
The same leaderboard and the same three workers, unchanged: "SORT: %CPU", "sensible_process 2%", "useful_process 1%", "idle_process 0%". A large flat vermilion clipboard with a cream sheet has slid in from the right and stands at the right of the square, partly overlapping the board's edge, with the text "PERFORMANCE REVIEW" in dark teal across its top. In the bottom row, the mustard worker has woken: sitting bolt upright, nightcap askew, pillow dropped, eyes wide, head turned toward the clipboard, staring at it. The two workers above carry on as before, not yet looking.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "The bottom process begins literally shovelling little CPU blocks into itself; its percentage climbs and its row moves upward. The other processes notice and imitate it. … End with all rows racing upward in CPU use."

~~~text
The same board, the same three workers and the same clipboard at the right. Every worker is now shovelling small vermilion square blocks into its own body with a little flat shovel, each still wearing its marker item (necktie, wrench, nightcap). The rows have reordered and the percentage bars are filling: first row, the mustard worker, "idle_process 81%"; second row, the dusty-lavender worker, "useful_process 63%"; third row, the mint-green worker, "sensible_process 47%". The workers lean forward, frantic but still plain flat shapes. A few loose vermilion blocks fly between the rows. The header still reads "SORT: %CPU".
~~~

### Frame 03 — end of cut 03 · "One process hits `99%` and receives a tiny trophy. Confetti appears for half a second; simultaneously the entire terminal freezes rigid. A boardroom stamp descends … `RECORD QUARTER`. Hold on the trophy beside a completely unusable system."

~~~text
The same board, frozen rigid. The top row reads "idle_process 99%", the mustard worker standing very upright, swollen and still, beside a tiny mustard trophy cup. The two rows below are stuck mid-motion with their shovels and blocks hanging unmoving in the air. Flat confetti pieces in mint green, mustard and dusty lavender hang motionless across the picture. Over the whole picture, tilted slightly, a large vermilion rectangular boardroom stamp mark with the text "RECORD QUARTER" in cream, as if pressed down onto the frozen scene. The header still reads "SORT: %CPU".
~~~

## Notes

- Text is the risk, and this scene has the most of it. Required, exactly as written: "SORT: %CPU", "sensible_process", "useful_process", "idle_process", their percentages, "PERFORMANCE REVIEW", "99%", "RECORD QUARTER". Reject any frame with a misspelling or an extra word. If a text item will not come out right after three tries, stop and ask the project owner; the guide's own advice is to leave a clean area and composite exact text in post.
- Invented by me, to make the scene readable: the reason the bottom worker acts (it works out the review only measures CPU), the three marker items (necktie, wrench, nightcap and pillow), the percentages in frame 02 (81%, 63%, 47%) and their order, and that the bottom worker is the one who reaches 99%. The beats say none of this. Reword freely.
- Frame 03 shows "frozen" by suspending the confetti and shovels mid-air under the stamp. If ChatGPT adds a frost or glitch effect, show it to the project owner rather than rejecting it.
- When a frame departs from a prompt on content (an extra prop, a different layout), show it to the project owner rather than rejecting it; reject only defects (wrong colour, non-flat background, broken shape, stray text).
