# Azrienoch

The app's typeface. Source: https://github.com/HereLiesAz/azrienoch (tag `1.1`, commit `42ee64c`), SIL Open
Font License 1.1 (`OFL.txt`). Azrienoch is a Modified Version of Jost. The 1.1 build has 915 glyphs and a
`wdth` axis that runs from 0 (Mono, every ASCII glyph 620 units wide) to 100.

`shared/src/commonMain/composeResources/font/azrienoch_{regular,semibold,extrabold,black}.ttf` are static
instances of `fonts/variable/Azrienoch-VF.ttf` made with `fontTools.varLib.instancer` at `wght` 400 / 600 /
800 / 900, `wdth` 100, `SERF` 0, `GRAD` 0.

Characters used in the app's strings that Azrienoch still lacks: `▸` and `✓`. They render in the system font.

`azrienoch_mono_regular.ttf` and `azrienoch_mono_bold.ttf` are the Mono cut (`wdth` 0) at `wght` 400 and 800, used by the full-screen terminal grid.
