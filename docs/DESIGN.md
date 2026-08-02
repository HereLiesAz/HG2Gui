# Design

The visual system is **Azphalt**: a warm printed yellow page, ink text, ten capsule hues, one
typeface (Jost), and a single primitive — the capsule. No borders. No shadows. No blur. No
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
as one bent shape. Arguments continue the same chain upward from their command.

The children are their own scroll region; the host does not move.

## 5. Motion

| | |
| --- | --- |
| Slide the stack away | 420ms, `cubic-bezier(.3, .05, .2, 1)` |
| Host drop | 420ms, `cubic-bezier(0, .9, .1, 1)` |
| Child turn | 520ms |
| Lift | final 10% of the turn |
| Cascade | strictly sequential — 520ms per step |
| Host rests at | 34% of the frame |
| Child pill | 34% wide, 30% in |
| Overhang | 62dp past the left edge |
| Row pitch | 20dp |

A child begins **exactly behind the pill before it** — the first behind the host, on the host's
row — so it is invisible at rest. It turns a full **360°** hinged on one end:
`TransformOrigin(0f, .5f)` for the first, `TransformOrigin(1f, .5f)` for the next, alternating
up the chain. It holds its predecessor's row for 90% of the arc and lifts exactly one row in
the final 10%, so the lift and the turn finish on the same frame.

Strictly sequential: a pill does not begin until the one before it has landed. Waiting children
are parked on the host's row underneath it — they do not exist on screen until their turn.

**Every change of stack is animated.** A pill never appears or vanishes on the spot; dismissing
a host slides the stack back in from off the left edge, 70ms apart.

Nothing fades. A pill is always fully opaque, even mid-turn. There are no hover or press states
— state is structural: a pill is a hue, or ink (open), or a 14% wash (idle). On an ink ground
that inverts: the open pill is yellow with an ink end-cap, so it never disappears into the page.

## 6. Scale

The menu holds fifty-one commands and their arguments, so it is drawn small: a pill is 17dp
tall with a 6sp label, stacked 20dp apart. This is the one place in Azphalt where type goes
below 9px — the pill, not the type, is the tap target, and it spans most of the screen.

Session tabs, command-line tokens and modifier keys sit at 8sp, uppercase, +0.09em.

## 7. Unchanged from Azphalt

Hue by hash, ten hues in assignment order, the darker mate on the end-cap. Ink for the open
pill, yellow for its label and cap. Record tile at 26dp radius, 9% ink. No borders, no shadows,
no blur, no icons, no hover states.
