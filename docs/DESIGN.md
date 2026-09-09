# Design

The visual system is **Azphalt**: printed page-like grounds, ink-forward typography, Jost, a fixed hue family, and rounded capsule/record geometry. Capsules are a major interaction primitive, especially for command composition, but they are not the only permissible visual element and they do not define every screen.

Purpose-built surfaces may use the visual language appropriate to their job: file browsing, terminal emulation, Guide reading, Adaptive TUI projection, package/audit inspection, dialogs, status/progress, and Android system affordances do not need to pretend they are all the same object.

Current implementation source of truth lives in `ui/menu/PillMenu.kt`, the relevant screen composables, and shared Azphalt tokens.

## 1. Pill menu anchoring

A pill is anchored by its right end and extends past the left edge of the screen. Lengths vary so a stack reads as a stack, and the right end stays within the designed frame fraction.

Labels and end-caps sit together at the right end. Command pills use the established uppercase, high-weight Azphalt type treatment.

## 2. Selecting a host

Selecting a host moves the current stack away, parks the host toward the left side of the frame, and drops it to the bottom as the anchor for the next layer.

Children cascade upward from that host. Each deeper selection becomes the next anchor; siblings leave and the new children arrive. The current trail records the selected path.

The stack is scrollable when its content exceeds the available height. Scroll settling uses the slot/reel behavior implemented by `rememberSlotFlingBehavior`; scrolling alone does not execute an item.

## 3. Trail

Selected children become content-sized trail crumbs beside the host. Earlier crumbs overlap later crumbs to preserve the shingled/fanned visual language.

Tapping a crumb returns composition to that point and reopens the corresponding layer.

## 4. Motion

Current menu timing constants are implemented in `PillMenu.kt`. The important motion rules are:

- stack transitions are structural rather than fade-based;
- child cascades remain sequential;
- host and child motion share one visual language;
- scroll settling is physically distinct from entry/exit choreography;
- animation state is keyed by stable node id rather than list index.

Where a screen grows from a source control, HG2Gui may use a source-anchored reveal rather than an unrelated cut. Files/path-picking currently use the wrap/perimeter reveal implementations.

## 5. Dedicated surfaces

A native surface should fit the object being manipulated.

Examples:

- **Files** may use rows, grids, media previews, storage summaries, selection controls, and file-specific affordances.
- **Guide** prioritizes readable prose while making real commands directly tappable for command composition.
- **Adaptive TUI Wrapper** renders menus, tabs, lists, tables, prompts, progress, and panes derived from the running terminal application.
- **Isolation Audit** groups observed process/file/network/authority activity rather than forcing it into command pills.
- **Shell presentation** may expose cwd, Git state, exit status, and other prompt semantics as interactive status elements.
- **Settings** is a vertically scrollable settings surface whose controls can extend beyond one viewport.

Visual elements, including symbols or icons, are chosen according to clarity and the surface's needs. Their use is not prohibited by a global doctrine.

## 6. Command composition

The pill stack is specifically optimized for command/option/value composition where hierarchical selection is useful.

It is not the universal input surface. HG2Gui also accepts:

- semantic completions;
- Guide command taps;
- typed open-ended operands;
- file/folder picker results;
- Adaptive TUI controls;
- shell prompt/status interactions;
- dedicated native feature screens.

Ordinary composed commands still require explicit **RUN**.

## 7. Scale and density

Runtime discovery means command/value collections can range from a handful of items to hundreds. Large collections must remain scrollable or otherwise browsable rather than assuming one screen of content.

Text size follows the screen's density needs while preserving usable touch targets. The global Settings text-scale control scales the terminal UI and can make Settings itself taller than the viewport; Settings therefore scrolls vertically.

## 8. Color and geometry

Azphalt retains its rotating grounds, ink, accent yellow, and fixed hue/cap palette as implemented in `PillMenu.kt`.

Capsules use fully rounded geometry; record surfaces use larger rounded cards. Individual screens may introduce additional geometry when the represented object requires it rather than forcing all UI into capsule form.

Color communicates structure and continuity, not security or semantic authority by itself. Execution authority, isolation, disabled state, and destructive actions must remain explicit in text/state and cannot rely on hue alone.

## 9. Accessibility and fallback

Native projections must never trap the user inside an incorrect interpretation.

- Adaptive TUI surfaces retain **RAW** terminal fallback.
- Open-ended values remain typeable.
- Scrollable screens must actually scroll when content exceeds the viewport.
- Interactive controls must keep meaningful text/semantics even when visual treatment changes.
- Elevated authority requires explicit foreground confirmation independent of styling.