#!/usr/bin/env python3
"""Regenerate Android + iOS launcher icons from art/app-icon-source.png.

Run from repo root:

    python3 art/generate-icons.py

Output:
- Android adaptive foreground PNGs at every density (image scaled to ~78%
  of canvas, padded with PinwoCream so the cream blends seamlessly with
  the source's own background) into mipmap-{density}/ic_launcher_foreground.png
- Android legacy ic_launcher.png and ic_launcher_round.png at every
  density (no padding — full source frame). The system applies its own
  round mask for the round variant on API < 26.
- iOS marketing icon at 1024x1024 into iosApp/.../AppIcon.appiconset/
"""

from pathlib import Path
from PIL import Image

REPO = Path(__file__).resolve().parent.parent
SRC = REPO / "art" / "app-icon-source.png"
ANDROID_RES = REPO / "composeApp" / "src" / "androidMain" / "res"
IOS_APPICON = REPO / "iosApp" / "iosApp" / "Assets.xcassets" / "AppIcon.appiconset"

CREAM = (255, 252, 248)  # PinwoCream — matches the source background

# Adaptive icon canvas is 108dp; raster densities are 1x..4x of that.
# We scale the source image to ~78% of the canvas so the entire frame
# (Berlin skyline at the bottom, X symbol top-left, etc.) survives the
# circular mask. Smaller % = safer; larger % = more icon, more crop risk.
SCALE_PCT = 78

ADAPTIVE_SIZES = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}

LEGACY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def gen_adaptive_foreground(source: Image.Image, canvas: int, out_path: Path) -> None:
    inner = canvas * SCALE_PCT // 100
    scaled = source.resize((inner, inner), Image.LANCZOS)
    bg = Image.new("RGBA", (canvas, canvas), CREAM + (255,))
    offset = ((canvas - inner) // 2, (canvas - inner) // 2)
    bg.paste(scaled, offset)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    bg.save(out_path, "PNG", optimize=True)


def gen_legacy(source: Image.Image, size: int, out_path: Path) -> None:
    scaled = source.resize((size, size), Image.LANCZOS)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    scaled.save(out_path, "PNG", optimize=True)


def main() -> None:
    if not SRC.exists():
        raise SystemExit(f"Source not found: {SRC}")

    print(f"Source: {SRC.relative_to(REPO)}")
    src = Image.open(SRC).convert("RGBA")
    print(f"  {src.size[0]}x{src.size[1]}")

    print("Android adaptive foregrounds:")
    for density, size in ADAPTIVE_SIZES.items():
        out = ANDROID_RES / f"mipmap-{density}" / "ic_launcher_foreground.png"
        gen_adaptive_foreground(src, size, out)
        print(f"  {out.relative_to(REPO)}  {size}x{size}")

    print("Android legacy + round icons:")
    for density, size in LEGACY_SIZES.items():
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            out = ANDROID_RES / f"mipmap-{density}" / name
            gen_legacy(src, size, out)
            print(f"  {out.relative_to(REPO)}  {size}x{size}")

    print("iOS marketing icon:")
    out = IOS_APPICON / "app-icon-1024.png"
    gen_legacy(src, 1024, out)
    print(f"  {out.relative_to(REPO)}  1024x1024")

    print("Done.")


if __name__ == "__main__":
    main()
