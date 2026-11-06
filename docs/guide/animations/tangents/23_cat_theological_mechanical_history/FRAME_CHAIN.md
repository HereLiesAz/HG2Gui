# Cat tangent — approved frame chain

Clean boundary frames for the 21 cuts. Clip `NN` starts on frame `NN-1` and ends on frame `NN`; frame 21 is the final hold. The renders are in `docs/` as `cat_tangent_frame_NN.png` (1254×1254, 1:1) with the character reference `cat_tangent_character_sheet.png`. How each frame was prompted is in [IMAGE_PROMPTS.md](IMAGE_PROMPTS.md).

This file is hand-kept. The generated `CLIPS.md`, `storyboard.md` and the per-clip `shots.md` still describe only the labelled `cat_NN.png` start stills, because the guide generators know no other source; they are not edited here.

| Cut | START | END | Clip packet | Narration beat |
|---|---|---|---|---|
| 01 | [frame 00](../../../../cat_tangent_frame_00.png) | [frame 01](../../../../cat_tangent_frame_01.png) | [clips/01](clips/01/README.md) | The word `cat` entered computing through a disagreement over which way up a cat goes. |
| 02 | [frame 01](../../../../cat_tangent_frame_01.png) | [frame 02](../../../../cat_tangent_frame_02.png) | [clips/02](clips/02/README.md) | This was not initially considered difficult. Cats corrected the record. |
| 03 | [frame 02](../../../../cat_tangent_frame_02.png) | [frame 03](../../../../cat_tangent_frame_03.png) | [clips/03](clips/03/README.md) | A cat regards whichever surface it occupies as the upper surface. |
| 04 | [frame 03](../../../../cat_tangent_frame_03.png) | [frame 04](../../../../cat_tangent_frame_04.png) | [clips/04](clips/04/README.md) | Turning the cat over merely gives the universe a new upper surface. |
| 05 | [frame 04](../../../../cat_tangent_frame_04.png) | [frame 05](../../../../cat_tangent_frame_05.png) | [clips/05](clips/05/README.md) | The Egyptians discovered this and mistook standardization for divinity. |
| 06 | [frame 05](../../../../cat_tangent_frame_05.png) | [frame 06](../../../../cat_tangent_frame_06.png) | [clips/06](clips/06/README.md) | The principle spread. |
| 07 | [frame 06](../../../../cat_tangent_frame_06.png) | [frame 07](../../../../cat_tangent_frame_07.png) | [clips/07](clips/07/README.md) | Observational research continued, mostly because the subject refused peer review. |
| 08 | [frame 07](../../../../cat_tangent_frame_07.png) | [frame 08](../../../../cat_tangent_frame_08.png) | [clips/08](clips/08/README.md) | Physicists tested gravity. Gravity worked. The cat declined to participate. |
| 09 | [frame 08](../../../../cat_tangent_frame_08.png) | [frame 09](../../../../cat_tangent_frame_09.png) | [clips/09](clips/09/README.md) | Further studies established a strong institutional preference for night. |
| 10 | [frame 09](../../../../cat_tangent_frame_09.png) | [frame 10](../../../../cat_tangent_frame_10.png) | [clips/10](clips/10/README.md) | Food selection was classified as constitutional law. |
| 11 | [frame 10](../../../../cat_tangent_frame_10.png) | [frame 11](../../../../cat_tangent_frame_11.png) | [clips/11](clips/11/README.md) | Hierarchy was accepted only when the cat occupied its highest point. |
| 12 | [frame 11](../../../../cat_tangent_frame_11.png) | [frame 12](../../../../cat_tangent_frame_12.png) | [clips/12](clips/12/README.md) | Computing shortened concatenate to `cat`; Unix writes file contents in sequence. |
| 13 | [frame 12](../../../../cat_tangent_frame_12.png) | [frame 13](../../../../cat_tangent_frame_13.png) | [clips/13](clips/13/README.md) | Curiously, the computer age isn't the first time this has happened. The last great cat religion began in ancient Egypt: geometry, to find the most perfectly shaped box that cats like to sit in. |
| 14 | [frame 13](../../../../cat_tangent_frame_13.png) | [frame 14](../../../../cat_tangent_frame_14.png) | [clips/14](clips/14/README.md) | Persia's mathematics found the area of a cat, and led to a complex written language based on cat scratches. |
| 15 | [frame 14](../../../../cat_tangent_frame_14.png) | [frame 15](../../../../cat_tangent_frame_15.png) | [clips/15](clips/15/README.md) | Greece's arm chair cats gave rise to arm-chair philosophers. |
| 16 | [frame 15](../../../../cat_tangent_frame_15.png) | [frame 16](../../../../cat_tangent_frame_16.png) | [clips/16](clips/16/README.md) | Science was born as the cat religion fueled Rome's pursuit of a new, invisible geometry called physics, to explain why cats always land on their feet. |
| 17 | [frame 16](../../../../cat_tangent_frame_16.png) | [frame 17](../../../../cat_tangent_frame_17.png) | [clips/17](clips/17/README.md) | The machine went on for thousands of years, driven by tangential concatenation. |
| 18 | [frame 17](../../../../cat_tangent_frame_17.png) | [frame 18](../../../../cat_tangent_frame_18.png) | [clips/18](clips/18/README.md) | Concatenation: con- means 'with', cat means cat, -nation means all of them. "With all of the cats." |
| 19 | [frame 18](../../../../cat_tangent_frame_18.png) | [frame 19](../../../../cat_tangent_frame_19.png) | [clips/19](clips/19/README.md) | For all their discoveries, Rome ironically failed to discover the true reason cats land on their feet: landing anywhere else can kill you. |
| 20 | [frame 19](../../../../cat_tangent_frame_19.png) | [frame 20](../../../../cat_tangent_frame_20.png) | [clips/20](clips/20/README.md) | Rome didn't fall in a day, |
| 21 | [frame 20](../../../../cat_tangent_frame_20.png) | [frame 21](../../../../cat_tangent_frame_21.png) | [clips/21](clips/21/README.md) | but when it did, Rome fell in exactly the way that cats don't. |

## Seedance

`python3 scripts/build_seedance_cat_tangent.py --out <dir>` builds a zip of seven 15-second generations (three cuts each). Each part opens on one frame in this chain and closes on another; neighbouring parts share a frame. `scripts/tests/test_build_seedance_cat_tangent.py` checks the pack against these sources.
