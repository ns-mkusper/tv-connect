#!/usr/bin/env python3
"""Compare before/after emulator screenshots and write visual diffs."""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Iterable

try:
    from PIL import Image, ImageChops
except ImportError as exc:  # pragma: no cover - exercised in CI if dependency is missing.
    raise SystemExit("Pillow is required. Install python3-pil before running this script.") from exc


def pngs(directory: Path) -> Iterable[Path]:
    return sorted(path for path in directory.glob("*.png") if path.is_file())


def normalize(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    canvas = Image.new("RGBA", size, (0, 0, 0, 0))
    canvas.paste(image.convert("RGBA"), (0, 0))
    return canvas


def write_diff(before_path: Path, after_path: Path, diff_path: Path) -> bool:
    before = Image.open(before_path)
    after = Image.open(after_path)
    size = (max(before.width, after.width), max(before.height, after.height))
    before_rgba = normalize(before, size)
    after_rgba = normalize(after, size)
    raw_diff = ImageChops.difference(before_rgba.convert("RGB"), after_rgba.convert("RGB"))
    if raw_diff.getbbox() is None:
        return False

    # Red overlay highlights changed pixels while retaining the after screenshot context.
    mask = raw_diff.convert("L").point(lambda value: 220 if value else 0)
    overlay = Image.new("RGBA", size, (255, 0, 0, 180))
    output = Image.alpha_composite(after_rgba, Image.composite(overlay, Image.new("RGBA", size), mask))
    diff_path.parent.mkdir(parents=True, exist_ok=True)
    output.save(diff_path)
    return True


def main() -> int:
    if len(sys.argv) != 4:
        print("usage: compare_emulator_screenshots.py BEFORE_DIR AFTER_DIR DIFF_DIR", file=sys.stderr)
        return 2

    before_dir = Path(sys.argv[1])
    after_dir = Path(sys.argv[2])
    diff_dir = Path(sys.argv[3])
    diff_dir.mkdir(parents=True, exist_ok=True)

    changed = 0
    missing = 0
    compared = 0
    for after_path in pngs(after_dir):
        before_path = before_dir / after_path.name
        if not before_path.exists():
            missing += 1
            continue
        compared += 1
        diff_path = diff_dir / f"{after_path.stem}.diff.png"
        if write_diff(before_path, after_path, diff_path):
            changed += 1

    print(f"Compared {compared} screenshot(s); {changed} changed; {missing} missing baseline(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
