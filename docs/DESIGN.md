# Design

The visual system is **Azphalt**: a warm printed yellow page, ink text, fourteen capsule hues,
one typeface (Jost), and a single primitive — the capsule. No borders. No shadows. No blur. No
icons. No emoji. Radii are 999px for anything pressable, 26px for a record tile.

This document specifies the one place HG2Gui departs from base Azphalt: the **pill menu**,
where the capsule stops being a row in a list and becomes a key.

Implemented in `ui/menu/PillMenu.kt`. Values below are the source of truth for that file.

## 1. Anchoring — pills run off the left edge

A pill is anchored by its **right end** and extends past the left edge of the screen — 62dp of
overhang, clipped by the frame. It never starts flush left. Lengths vary so a stack reads as a
stack, and the right end never passes **two thirds** of the screen width.

## 2. Reading order — the label rides the right end

Label and end-cap sit together at the right end, in that order. The long left run of colour is
empty. Type is 800 uppercase at +0.09em, as everywhere else in Azphalt; only the alignment
changes. A pill with no end-cap still right-aligns its label.

## 3. Selecting a host

Tapping a host sends the entire stack further left: every unselected pill leaves the screen,
and the host stops with its **right end parked at 34%** of the frame — whatever its length —
so only its label and end-cap remain visible. It then **drops to the bottom of the screen**,
`rowsBelow × pitch`, and stays pinned there.

## 4. Children cascade upward

Children cascade **upward** from the host, all the same size (34% wide, starting at 30% —
just inside the host's right end). The first child sits on the host's own row, so the two read
as one bent shape.

Arguments repeat this exact choreography, one level at a time, however deep the tree goes.
Tapping a pill with its own children makes it the new anchor: its siblings **leave**, the same
motion the root stack uses when a host is chosen, while its children cascade in next to it —
the first sitting on *its* row, just as the first child always sits on its own anchor's row. A
band's height never grows with depth: each level is its own fresh cascade, not more pills piled
onto the one before it. Tapping the anchor again undoes the drill — its siblings **enter** back,
the same motion the root stack uses to re-enter.

The children are their own scroll region; the host does not move.

## 4a. The trail

A picked child doesn't just cascade its own children in — it also drops out of the band and
settles beside the host as a **trail crumb**, the record of what's been picked so far. The
trail starts at 24% of the frame, just clear of the host's right end, and runs left to right in
pick order. Unlike every other pill (sized as a fraction of the screen), a crumb is sized by its
own content, with a 56dp floor so a short label like `ls` doesn't shrink-wrap smaller than
everything around it. Tapping a crumb pops the trail back to just before it and re-opens that
pill's own band — each level is its own fresh cascade, never more pills piled onto the one
before it.

## 5. Motion

| | |
| --- | --- |
| Slide the stack away | 140ms, linear |
| Host drop | 140ms, linear |
| Child turn (swing) | 173ms, linear |
| Lift | final 10% of the turn |
| Cascade | strictly sequential — 173ms per step |
| Host rests at | 34% of the frame |
| Child pill | 34% wide, 30% in |
| Trail starts at | 24% of the frame |
| Trail crumb | content-sized, 56dp floor |
| Overhang | 62dp past the left edge |
| Row pitch | 20dp |
| Pick grows to | 1.15× before settling back to 1× |

Every motion in the menu is **linear** — there is no easing curve anywhere; an eased pill reads
as a bug at this speed.

A child begins **exactly behind the pill before it** — the first behind the host, on the host's
row — so it is invisible at rest. It turns a full **360°** hinged on one end:
`TransformOrigin(0f, .5f)` for the first, `TransformOrigin(1f, .5f)` for the next, alternating
up the chain. It holds its predecessor's row for 90% of the arc and lifts exactly one row in
the final 10%, so the lift and the turn finish on the same frame.

Strictly sequential: a pill does not begin until the one before it has landed. Waiting children
are parked on the host's row underneath it — they do not exist on screen until their turn.

**Every change of stack is animated.** A pill never appears or vanishes on the spot; dismissing
a host plays the exact same arrival as opening the app — one column, one clock, no per-pill
stagger, because returning to a stack and arriving at it are the same event.

Nothing fades. A pill is always fully opaque, even mid-turn. There are no hover or press states
— state is structural: a pill is a hue, or ink (open), or a 14% wash (idle). On an ink ground
that inverts: the open pill is yellow with an ink end-cap, so it never disappears into the page.

Animation state is remembered against a node's **id**, never its index — a contextual root can
appear or vanish between frames, and a keyed-by-position pill would inherit a stranger's motion.

## 5a. The pill becomes the page

Opening the Files screen doesn't cut to a new screen — the **FILES** pill itself grows into it.
Simplified from the source spec's own multi-stage "stretch, snap, fly, run the perimeter, flood"
sequence into two continuous beats (`PillWrapReveal.kt`):

| | |
| --- | --- |
| Wrap | 640ms, `cubic-bezier(0,.9,.1,1)` — the pill's own rect interpolates out to the full screen, closing a hue-coloured frame around it |
| Flood | 420ms, same easing — a bottom-to-top wipe reveals the file explorer already inside the closed frame |
| Header drop | 360ms — the top bar (close/parent/count chips) drops in from above the top edge |
| Footer pop | 360ms, same clock — the bottom action bar rises in from below the bottom edge |

The frame's border stays visible for as long as the screen is open, tying its hue back to
whichever pill opened it. Closing plays the same two beats in reverse. Whenever there's a level
open above the root, a yellow **…** chip drops in with the rest of the header — tap it to close
the deepest open level, same as tapping its own capsule again.

The Select File/Folder pill runs the fuller, un-simplified version of the same source sequence
(`PillPerimeterReveal.kt`) instead: from the pill's own trail crumb, one edge grows at a time -
bottom, right, top, left, 260ms apiece - closing the loop back over its own start, with the flood
wipe (this time top-to-bottom) starting the instant that last edge does. It gets the closer read
because it opens from an arbitrary crumb position rather than a fixed root pill, so a single
rect-interpolation (as `PillWrapReveal` does) would visibly cut a corner instead of tracing one.

## 6. Scale

The menu's size isn't fixed — the shell categories are discovered live from what's actually
installed, so a band can hold anywhere from a handful of built-ins to hundreds of real
binaries. It is drawn small throughout regardless: a pill is 17dp tall with a 6sp label, stacked
20dp apart. This is the one place in Azphalt where type goes below 9px — the pill, not the
type, is the tap target, and it spans most of the screen. A category large enough to strain the
fan-out animation is capped per category rather than rendered (or hung on) unbounded, with a
trailing "+N more" pill marking what was left out.

Session tabs, command-line tokens and modifier keys sit at 8sp, uppercase, +0.09em.

## 7. Unchanged from Azphalt

Hue by hash, fourteen hues in assignment order (the original ten sit on the default ground; four
more - gray, sage, tan, brown - extend the set for the rarer grounds and category recolors), the
darker mate on the end-cap. Ink for the open pill, yellow for its label and cap. Record tile at
26dp radius, 9% ink. No borders, no shadows, no blur, no icons, no hover states.
