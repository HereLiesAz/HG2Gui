# Animation content index

All Guide animation production material is organized under `docs/guide/animations/`.
Original source files remain where they were so existing repository links do not break.

## Boundary audit

- Animations: **42**
- Clips / scenes: **173**
- START/END boundary slots: **346**
- Boundary slots backed by real organized image files: **116**
- Missing or unresolved boundary slots: **230**
- Detailed actionable list: [`ANIMATION_TODO.md`](ANIMATION_TODO.md)

## Organization

Each animation directory contains its complete animation text, storyboard/cut information, scene folders, and animation-specific references when available.
Each scene has a `frames/` directory with explicit START and END records. Real boundary images are normalized to `start.*` and `end.*`; missing artwork is recorded rather than fabricated.

### Animation-specific loose media moved out of the shared bucket

- `01_pwd` — 6 source asset(s) organized with the animation
- `02_pkg_install` — 8 source asset(s) organized with the animation
- `02_pkg_install_small_empire_of_dependencies` — 12 source asset(s) organized with the animation
- `03_apt_get_update` — 2 source asset(s) organized with the animation
- `22_find_cartographical_disaster` — 6 source asset(s) organized with the animation
- `23_cat_theological_mechanical_history` — 12 source asset(s) organized with the animation
- `34_mv_elevator_incident` — 12 source asset(s) organized with the animation
- `echo` — 8 source asset(s) organized with the animation
- `ls` — 8 source asset(s) organized with the animation
- `ls_tangent` — 12 source asset(s) organized with the animation

Guide-wide film/style screenshots, logos, and banners remain under `project/` because they are not specific to one animation.
