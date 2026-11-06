# 04_nano — image prompts

Four boundary frames for the three cuts of `04_nano` (15 seconds). Frame `NN` is the START of cut `NN+1`; the same image is the END of cut `NN`. Frame 03 is the end of the last cut.

This is the next canonical animation without frames: `01_pwd`, `02_pkg_install` and `03_apt_get_update` are done. It is next in order and, at 15 seconds, among the shortest, so it also tests the routine before the longer ones.

## How to use

1. Start one new ChatGPT conversation. Attach `docs/03_apt_get_update_frame_00.png` and say: "This is the style to match: flat matte cut-paper pictogram, one flat background colour. Do not copy its content."
2. Paste the **style block**, then the prompt for frame 00. Generate until it is right.
3. For each later frame, attach the previous approved frame, paste the style block again, then the frame prompt. Say: "Keep the same capsule shapes, colours and lettering as the previous frame."
4. Save approved frames as `docs/04_nano_frame_00.png` to `docs/04_nano_frame_03.png`. The Seedance pack builder (`scripts/build_seedance_packs.py`) picks them up by that name; no other change is needed.

## Colours

Background for the whole animation: deep cobalt `#183D7A`, taken from this entry's generated prompts. Palette: mustard `#D9A12B`, violet `#6B4FA0`, coral red `#D9604C`, pale cyan `#BFE6EA`. The hex values for violet, coral and cyan are my choices; the guide names the colours but gives only the background's hex.

## Style block (paste before every frame prompt)

~~~text
Square image, 1:1. One single flat deep cobalt (#183D7A) background fills the whole square edge to edge. Flat 2D screen-printed cut-paper pictogram style: simple opaque cutout shapes, economical geometric silhouettes, matte colour, slightly imperfect printed edges, large areas of empty space. No gradients, no gloss, no drop shadows, no 3D shading, no photographic detail. Only these colours besides the background: mustard, violet, coral red, pale cyan. No header, no tabs, no border, no frame, no captions, no other text. The only lettering allowed is the lowercase word "nano" in a heavy geometric sans-serif, spelled exactly n-a-n-o, and only where the frame prompt says so. Deadpan, calm and absurd, shown with complete seriousness.
~~~

## Frames

### Frame 00 — start of cut 01 · "The single word `nano` sits centered like a labeled object."

~~~text
At the exact centre, one closed, rounded, capsule-shaped mustard label lying horizontally, with the lowercase word "nano" in deep cobalt letters across it. Nothing else in the picture. A great deal of empty cobalt around it.
~~~

### Frame 01 — end of cut 01 / start of cut 02 · "It unfolds like a hinged capsule, revealing a smaller `nano` inside … End mid-zoom with three visible nested layers."

~~~text
A close, flat view straight into nested capsules, as if the camera has zoomed inward. The outermost mustard capsule is opened flat on a hinge and is cropped by the edges of the square. Inside it, an opened violet capsule. Inside that, an opened coral-red capsule. At the very centre, a small closed pale-cyan capsule with the lowercase word "nano" in deep cobalt on it. Each capsule is a clean flat shape with a thin cobalt gap around the next one, no shading.
~~~

### Frame 02 — end of cut 02 / start of cut 03 · "A tiny footer appears along the bottom edge: `^X Exit`. The camera overshoots past it … End with the footer a speck."

~~~text
The same nested capsules, zoomed so far in that they have become fine concentric stripes in alternating mustard, violet, coral red and pale cyan, getting thinner toward the centre, which is a pale-cyan point too small to read. Along the bottom edge, a single tiny pale-cyan speck, smaller than a grain, stands where a footer would be. It carries no legible text. Most of the square is plain cobalt.
~~~

### Frame 03 — end of cut 03 · "A gigantic simplified human finger … misses the tiny exit mark … and presses the entire nested universe flat like a button. Hard hold on the squashed cosmos."

~~~text
From the top edge of the square, an enormous simplified human finger in flat coral red, pointing straight down, with a plain flat nail and no detail, is pressed against the centre. Under its tip, the whole nested stack is squashed into a thin flat band of stripes in mustard, violet and pale cyan. A few finger-widths to the right of the fingertip, a single tiny pale-cyan speck sits untouched on the cobalt: the exit mark it missed. Nothing else.
~~~

## Notes

- Text is the risk. Frames 00 to 02 carry the word "nano"; reject any frame where it is misspelled, mirrored, or repeated on layers that should be plain. Frame 02's footer is deliberately illegible, so a legible `^X Exit` there is also a miss.
- The finger in frame 03 is coral red so it separates from the cobalt; if it reads as a carrot, say "no knuckle, no wrinkles, just a rounded rectangle with a nail".
- Frame 01's three nested layers are mustard, violet and coral red, with the pale-cyan capsule at the centre. Reword freely if ChatGPT draws more or fewer layers; the beat only needs "several, clearly nested".
- When a frame departs from a prompt on content (an extra layer, a different finger colour), show it to the project owner rather than rejecting it; reject only defects (wrong colour, broken shape, stray text).
