#!/usr/bin/env python3
"""Checks for scripts/build_seedance_packs.py: every canonical animation builds into a faithful pack."""
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
CANONICAL = ROOT / "docs/guide/animations/canonical"


def load():
    location = ROOT / "scripts/build_seedance_packs.py"
    spec = importlib.util.spec_from_file_location("build_seedance_packs", location)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module  # dataclasses resolves string annotations through sys.modules
    spec.loader.exec_module(module)
    return module


class SeedancePackTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.mod = load()
        cls.tmp = tempfile.TemporaryDirectory()
        cls.out = Path(cls.tmp.name)
        cls.mod.main(["--out", str(cls.out)])

    @classmethod
    def tearDownClass(cls) -> None:
        cls.tmp.cleanup()

    def folders(self):
        return sorted(p.name for p in CANONICAL.iterdir() if (p / "animation.md").is_file())

    def read(self, folder: str, name: str) -> str:
        with zipfile.ZipFile(self.out / f"{folder}_seedance2.5.zip") as zf:
            return zf.read(f"{folder}_seedance2.5/{name}").decode("utf-8")

    def test_cut_sheet_times_are_seconds_not_minutes(self):
        self.assertEqual(self.mod.clock("05:00"), "5s")
        self.assertEqual(self.mod.clock("00:00"), "0s")
        self.assertEqual(self.mod.clock("20:00"), "20s")
        self.assertEqual(self.mod.clock("02:50"), "2.5s")

    def test_one_zip_per_canonical_animation(self):
        self.assertEqual(len(self.folders()), 35)
        for folder in self.folders():
            self.assertTrue((self.out / f"{folder}_seedance2.5.zip").is_file(), folder)

    def test_every_pack_is_one_square_headerless_generation_covering_the_whole_duration(self):
        for folder in self.folders():
            prompt = self.read(folder, "prompt.txt")
            source = (CANONICAL / folder / "animation.md").read_text(encoding="utf-8")
            seconds = int(re.search(r"Total duration:\*\* (\d+) seconds", source).group(1))
            self.assertIn("Exactly 1:1 square", prompt, folder)
            self.assertIn("NO header, NO tabs", prompt, folder)
            self.assertNotIn("2.35", prompt, folder)
            self.assertIn(f"{seconds} seconds, one continuous generation", prompt, folder)
            windows = re.findall(r"Beat \d+ of \d+ — (\S+)–(\S+)", prompt)
            self.assertEqual(windows[0][0], "0s", folder)
            self.assertEqual(windows[-1][1], f"{seconds}s", folder)

    def test_scene_description_and_script_are_complete(self):
        for folder in self.folders():
            prompt = self.read(folder, "prompt.txt")
            source = (CANONICAL / folder / "animation.md").read_text(encoding="utf-8")
            for cut in re.findall(r"## Cut \d+ — .*?\n\n(.*?)\n\n### Ready", source, re.S):
                self.assertIn(cut.strip(), prompt, folder)
            sequence = re.search(r"## Source animation sequence\n\n(.*?)\n\n## Sequence lock", source, re.S).group(1)
            for paragraph in sequence.replace("**", "").split("\n\n"):
                text = paragraph.replace("**Editor's note:**", "").replace("Editor's note:", "").strip()
                self.assertIn(text, prompt, f"{folder}: {text[:40]}")

    def test_images_are_byte_identical_to_repo_sources_and_every_one_is_tagged(self):
        expected = {"01_pwd": 6, "02_pkg_install": 5, "03_apt_get_update": 4, "04_nano": 4, "05_top": 4}
        repo_hashes = {hashlib.md5(p.read_bytes()).hexdigest() for p in (ROOT / "docs").rglob("*.png")}
        for folder in self.folders():
            with zipfile.ZipFile(self.out / f"{folder}_seedance2.5.zip") as zf:
                images = [n for n in zf.namelist() if "/references/" in n and not n.endswith("/")]
                self.assertEqual(len(images), expected.get(folder, 0), folder)
                prompt = zf.read(f"{folder}_seedance2.5/prompt.txt").decode("utf-8")
                for position, name in enumerate(sorted(images), start=1):
                    self.assertIn(hashlib.md5(zf.read(name)).hexdigest(), repo_hashes, name)
                    self.assertIn(f"@Image{position}:", prompt, name)

    def test_boundary_packs_name_the_opening_middle_and_closing_frames(self):
        prompt = self.read("02_pkg_install", "prompt.txt")
        self.assertIn("@Image1: OPENING FRAME: the exact composition at 0s.", prompt)
        self.assertIn("@Image3: the exact composition reached at 10s", prompt)
        self.assertIn("@Image5: CLOSING FRAME: the exact composition at 20s.", prompt)

    def test_pwd_follows_the_images_background_and_forbids_the_model_sheet_labels(self):
        prompt = self.read("01_pwd", "prompt.txt")
        self.assertIn("mustard (#E39B1D)", prompt)
        self.assertNotIn("#101D2D", prompt)
        self.assertIn("Never paint any text label from the model sheet", prompt)
        self.assertIn("Editor's note (context only", prompt)


if __name__ == "__main__":
    unittest.main()
