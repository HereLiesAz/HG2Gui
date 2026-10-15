#!/usr/bin/env python3
"""Regression checks for HG2Gui animation documentation and source provenance."""
from __future__ import annotations

import hashlib
import importlib.util
import json
import re
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GUIDE = ROOT / "docs/guide"
PRESENT = {"present", "present_duplicate_identical"}


def load(name: str, location: Path):
    spec = importlib.util.spec_from_file_location(name, location)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot import {location}")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


class AnimationDocumentationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.manifest = json.loads((GUIDE / "MANIFEST.json").read_text(encoding="utf-8"))

    def test_all_clips_have_script_shots_and_prompt(self) -> None:
        total = 0
        for collection in ("canonical_animations", "tangent_animations", "legacy_animations"):
            for animation in self.manifest[collection]:
                folder = ROOT / animation["folder"]
                self.assertTrue((folder / "CLIPS.md").is_file())
                for scene in animation["scenes"]:
                    clip = folder / "clips" / f"{int(scene['scene']):02d}"
                    for name in ("README.md", "script.md", "shots.md", "prompt.md"):
                        self.assertTrue((clip / name).is_file(), str(clip / name))
                    total += 1
        self.assertEqual(total, 173)

    def test_guide_indexes_have_no_broken_local_links(self) -> None:
        paths = [GUIDE / "CLIP_PRODUCTION_INDEX.md", GUIDE / "ANIMATION_TODO.md"]
        paths += list((GUIDE / "animations").rglob("CLIPS.md"))
        self.assertEqual(len(paths), 44)
        for source in paths:
            content = source.read_text(encoding="utf-8")
            for raw in re.findall(r"\]\(([^)]+)\)", content):
                if "://" in raw or raw.startswith("#"):
                    continue
                target = raw.split("#", 1)[0]
                self.assertTrue((source.parent / target).exists(), f"{source}: broken link to {raw}")

    def test_inventory_is_exactly_regenerated(self) -> None:
        generator = load("animation_todo_test", ROOT / "scripts/build_animation_todo.py")
        expected = generator.render(self.manifest)
        self.assertEqual((GUIDE / "ANIMATION_TODO.md").read_text(encoding="utf-8"), expected)

    def test_pkg_install_shared_boundary_has_canonical_source(self) -> None:
        root_frame = ROOT / "docs/02_pkg_install_frame_02.png"
        self.assertTrue(root_frame.is_file())
        paths = [
            GUIDE / "animations/canonical/02_pkg_install/clips/02/end.png",
            GUIDE / "animations/canonical/02_pkg_install/clips/03/start.png",
            GUIDE / "animations/canonical/02_pkg_install/scenes/02/frames/end.png",
            GUIDE / "animations/canonical/02_pkg_install/scenes/03/frames/start.png",
        ]
        expected = hashlib.sha256(root_frame.read_bytes()).digest()
        for path in paths:
            self.assertTrue(path.is_file(), str(path))
            self.assertEqual(hashlib.sha256(path.read_bytes()).digest(), expected, str(path))

    def test_tangent_images_are_annotated_starts_not_endpoints(self) -> None:
        for animation in self.manifest["tangent_animations"]:
            for scene in animation["scenes"]:
                frames = scene["storyboard"]
                self.assertIn(frames["start"]["status"], PRESENT)
                self.assertEqual(frames["end"]["status"], "unresolved")
                self.assertIsNone(frames["end"]["local"])
                clip = ROOT / animation["folder"] / "clips" / f"{int(scene['scene']):02d}"
                self.assertTrue((clip / "start.png").is_file())
                self.assertFalse((clip / "end.png").exists())

    def test_legacy_prompts_do_not_impose_unverified_timing(self) -> None:
        for animation in self.manifest["legacy_animations"]:
            for scene in animation["scenes"]:
                clip = ROOT / animation["folder"] / "clips" / f"{int(scene['scene']):02d}"
                prompt = (clip / "prompt.md").read_text(encoding="utf-8")
                self.assertIn("runtime", prompt.lower())
                self.assertNotIn("EXACTLY 5.0 seconds", prompt)
                self.assertNotRegex(prompt, r"\b\d+(?:\.\d+)?[–-]\d+(?:\.\d+)?s\b")


    def test_readme_refresh_preserves_curated_notes(self) -> None:
        builder = load("animation_readme_test", ROOT / "scripts/build_animation_clip_packets.py")
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "README.md"
            path.write_text(
                "# Clip\n\n- START: **missing** — artist note\n"
                "- END: [end.png](end.png)\n\nKeep this custom paragraph.\n",
                encoding="utf-8",
            )
            builder.refresh_readme(
                path, "unused fallback", "- START: **missing** — other note",
                "- END: [end.png](end.png)",
            )
            self.assertIn("artist note", path.read_text(encoding="utf-8"))
            builder.refresh_readme(
                path, "unused fallback", "- START: [start.png](start.png)",
                "- END: [end.png](end.png)",
            )
            updated = path.read_text(encoding="utf-8")
            self.assertIn("- START: [start.png](start.png)", updated)
            self.assertIn("Keep this custom paragraph.", updated)

    def test_shot_status_refresh_preserves_visual_directions(self) -> None:
        builder = load("animation_shots_test", ROOT / "scripts/build_animation_clip_packets.py")
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "shots.md"
            path.write_text(
                "# Clip\n\n## START shot\n\nA hand opens the door.\n\n"
                "**START** — NO IMAGE\n\n- Source: not supplied\n"
                "- Type: no image supplied\n\n"
                "## END shot\n\nThe door is open.\n\n"
                "**END** — NO IMAGE\n\n- Source: not supplied\n"
                "- Type: no image supplied\n",
                encoding="utf-8",
            )
            boundaries = {
                "start": {"sources": ["docs/example.png"]},
                "end": {"sources": []},
            }
            builder.refresh_shots(path, boundaries, {"start": True, "end": False}, "canonical")
            updated = path.read_text(encoding="utf-8")
            self.assertIn("A hand opens the door.", updated)
            self.assertIn("**START** — [image](start.png)", updated)
            self.assertIn("- Source: docs/example.png", updated)
            self.assertIn("**END** — NO IMAGE", updated)


    def test_removed_source_does_not_leave_stale_scene_frame(self) -> None:
        organizer = load("animation_organizer_test", ROOT / "scripts/organize_guide_material.py")
        with tempfile.TemporaryDirectory() as folder:
            scene_dir = Path(folder) / "scenes" / "01"
            frame_dir = scene_dir / "frames"
            frame_dir.mkdir(parents=True)
            old = frame_dir / "start.png"
            old.write_bytes(b"stale generated frame")
            boundary = organizer.materialize_boundary(
                "missing_frame.png", "start", scene_dir, {},
                "explicit_cut_sheet_boundary",
            )
            self.assertEqual(boundary["status"], "missing")
            self.assertFalse(old.exists())

    def test_packet_rebuild_does_not_dirty_curated_documents(self) -> None:
        builder = load("animation_packets_test", ROOT / "scripts/build_animation_clip_packets.py")
        paths = [GUIDE / "CLIP_PRODUCTION_INDEX.md"]
        for collection in ("canonical_animations", "tangent_animations", "legacy_animations"):
            for animation in self.manifest[collection]:
                folder = ROOT / animation["folder"]
                paths.append(folder / "CLIPS.md")
                for scene in animation["scenes"]:
                    clip = folder / "clips" / f"{int(scene['scene']):02d}"
                    paths.extend(clip / name for name in ("README.md", "script.md", "shots.md", "prompt.md"))
        before = {path: hashlib.sha256(path.read_bytes()).digest() for path in paths}
        self.assertEqual(builder.build()[0], 173)
        for path, digest in before.items():
            self.assertEqual(hashlib.sha256(path.read_bytes()).digest(), digest, str(path))


if __name__ == "__main__":
    unittest.main()
