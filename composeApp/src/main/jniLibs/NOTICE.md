# Bundled native binaries (arm64-v8a)

The `.so` files under `arm64-v8a/` are **not** original HG2Gui code. They are
executables and libraries built by the [Termux](https://termux.dev) project
(from [termux-packages](https://github.com/termux/termux-packages)) and renamed
to `lib*.so` so Android installs them into the app's native library directory
(see `useLegacyPackaging` in `composeApp/build.gradle.kts`). Naming:
`libbin_<tool>.so` is `$PREFIX/bin/<tool>`, `liblib_*` is a library, and
`liblibexec_*` is a `libexec` helper.

## Licences

They include GPL, LGPL and other free-software programs (bash, coreutils, GNU
awk, grep, sed, tar, gzip, findutils, apt, dpkg, nano, curl and others, and
the libraries they link). Each remains under its own licence; the GPL
programs are distributed under the GNU General Public License. This notice
does not change those licences.

## Source offer

The complete corresponding source for every binary here is the package
recipe and upstream source referenced by termux-packages:

- https://github.com/termux/termux-packages (build recipes and patches,
  under `packages/<name>/build.sh`)
- Upstream sources are the URLs named in each `build.sh` (`TERMUX_PKG_SRCURL`).

If you cannot obtain the source from those locations, write to the repository
owner (see the project README) and the source will be provided for at least
three years after you received the binary.

## Integrity

`SHA256SUMS` lists the SHA-256 of every file in `arm64-v8a/` (paths are
relative to that directory). Verify with:

    cd composeApp/src/main/jniLibs/arm64-v8a && sha256sum -c ../SHA256SUMS

Regenerate it whenever the binaries are refreshed.
