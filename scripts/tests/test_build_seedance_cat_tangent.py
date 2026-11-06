#!/usr/bin/env python3
"""Checks for scripts/build_seedance_cat_tangent.py: seven faithful 15-second parts covering all 21 cuts."""
from __future__ import annotations

import hashlib
import importlib.util
import re
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ROOT_NAME = "23_cat_theological_mechanical_history_seedance2.5"


def load():
    spec = importlib.util.spec_from_file_location("build_seedance_cat_tangent", ROOT / "scripts/build_seedance_cat_tangent.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class CatTangentPackTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.mod = load()
        cls.tmp = tempfile.TemporaryDirectory()
        cls.out = Path(cls.tmp.name)
        assert cls.mod.main(["--out", str(cls.out)]) == 0
        cls.zf = zipfile.ZipFile(cls.out / f"{ROOT_NAME}.zip")
        cls.parts = sorted({n.split("/")[1] for n in cls.zf.namelist() if "/part_" in n})

    @classmethod
    def tearDownClass(cls) -> None:
        cls.zf.close()
        cls.tmp.cleanup()

    def prompt(self, part: str) -> str:
        return self.zf.read(f"{ROOT_NAME}/{part}/prompt.txt").decode("utf-8")

    def images(self, part: str) -> list[str]:
        return sorted(n for n in self.zf.namelist() if f"/{part}/references/" in n)

    def test_seven_parts_of_fifteen_seconds(self):
        self.assertEqual(len(self.parts), 7)
        for part in self.parts:
            prompt = self.prompt(part)
            self.assertIn("one continuous 15-second animation in 3 consecutive beats", prompt, part)
            self.assertIn("Exactly 1:1 square", prompt, part)
            self.assertIn("NO header, NO tabs", prompt, part)
            self.assertNotIn("2.35", prompt, part)

    def test_every_cut_is_in_exactly_one_part_in_order(self):
        beats = self.mod.parse_beats()
        self.assertEqual(len(beats), 21)
        seen = []
        for part in self.parts:
            seen += [t for t in re.findall(r"Story beat: (.+)", self.prompt(part))]
        self.assertEqual(seen, [beats[n] for n in range(1, 22)])

    def test_each_part_has_four_boundary_frames_and_the_character_sheet_all_tagged(self):
        for part in self.parts:
            names = self.images(part)
            self.assertEqual(len(names), 5, part)
            prompt = self.prompt(part)
            for position in range(1, 6):
                self.assertIn(f"@Image{position}:", prompt, part)
            self.assertNotIn("@Image6", prompt, part)
            self.assertTrue(names[-1].endswith("05_character_sheet.png"), part)

    def test_images_are_byte_identical_to_the_repo_files(self):
        sheet = (ROOT / "docs/cat_tangent_character_sheet.png").read_bytes()
        for part in self.parts:
            for name in self.images(part):
                data = self.zf.read(name)
                match = re.search(r"_frame_(\d\d)_at_", name)
                expected = sheet if name.endswith("character_sheet.png") else (ROOT / f"docs/cat_tangent_frame_{match.group(1)}.png").read_bytes()
                self.assertEqual(hashlib.md5(data).hexdigest(), hashlib.md5(expected).hexdigest(), name)

    def test_each_part_opens_on_the_frame_the_previous_part_closes_on(self):
        for before, after in zip(self.parts, self.parts[1:]):
            last = hashlib.md5(self.zf.read(self.images(before)[3])).hexdigest()
            first = hashlib.md5(self.zf.read(self.images(after)[0])).hexdigest()
            self.assertEqual(last, first, f"{before} -> {after}")
        self.assertIn("frame_00_at_0s", self.images(self.parts[0])[0])
        self.assertIn("frame_21_at_15s", self.images(self.parts[-1])[3])

    def test_frame_descriptions_come_from_image_prompts(self):
        frames = self.mod.parse_frames()
        self.assertEqual(sorted(frames), list(range(22)))
        self.assertIn("(#D9A12B)", frames[12].background.upper())
        self.assertIn(frames[13].composition, self.prompt(self.parts[4]))
        self.assertIn("cuneiform", frames[13].composition)


if __name__ == "__main__":
    unittest.main()
