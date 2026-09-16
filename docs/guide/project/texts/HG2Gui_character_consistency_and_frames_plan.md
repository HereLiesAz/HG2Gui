# HG2Gui — Character Consistency & Boundary Frame Pipeline

- 35 Guide entries
- 117 five-second video cuts
- 152 unique boundary frames, not 234; adjacent cuts share the exact same image file
- 35 sequence reference boards
- Output aspect ratio: 1:1
- One flat background color per entry; no generated title/header/tab/border/UI

## Required generation order

For each entry, generate its sequence reference board first. Then generate `frame_00` through `frame_N` from that same board. Then generate Cut 1 from `frame_00` → `frame_01`, Cut 2 from the literal same `frame_01` → `frame_02`, and so on.

Do not ask a video model to recreate the character at the start of the next clip. Give it the actual previous frame. That single change eliminates the largest source of identity drift.
