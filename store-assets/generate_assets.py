"""Generate Google Play Store assets for Birdo VPN."""
from PIL import Image, ImageDraw, ImageFont
import math
import os

OUT = r"W:\vpn\birdo-client-android\store-assets"
os.makedirs(OUT, exist_ok=True)

# ── Colours ──────────────────────────────────────────────────────────
GRAD_START = (139, 92, 246)   # #8B5CF6
GRAD_END   = (99, 102, 241)   # #6366F1
WHITE      = (255, 255, 255)
WHITE_A    = (255, 255, 255, 220)


def gradient_bg(w, h):
    """Create a diagonal linear gradient image."""
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            t = (x / w + y / h) / 2
            r = int(GRAD_START[0] + (GRAD_END[0] - GRAD_START[0]) * t)
            g = int(GRAD_START[1] + (GRAD_END[1] - GRAD_START[1]) * t)
            b = int(GRAD_START[2] + (GRAD_END[2] - GRAD_START[2]) * t)
            img.putpixel((x, y), (r, g, b, 255))
    return img


def draw_wifi_icon(draw, cx, cy, scale=1.0):
    """Draw concentric WiFi arcs + center dot."""
    # Arc radii (from foreground XML, mapped to pixels)
    arcs = [
        (24 * scale, 4 * scale),   # outer
        (16 * scale, 4 * scale),   # middle
        (8 * scale, 4 * scale),    # inner
    ]
    for radius, thickness in arcs:
        bbox = [cx - radius, cy - radius, cx + radius, cy + radius]
        draw.arc(bbox, start=180, end=360, fill=WHITE, width=max(int(thickness), 2))

    # Center dot
    dot_r = 3 * scale
    draw.ellipse([cx - dot_r, cy - dot_r, cx + dot_r, cy + dot_r], fill=WHITE)


# ─── 1. App Icon 512×512 ────────────────────────────────────────────
print("Generating app icon 512x512...")
SIZE = 512
icon = gradient_bg(SIZE, SIZE)
draw = ImageDraw.Draw(icon)

# Rounded corners mask
mask = Image.new("L", (SIZE, SIZE), 0)
mask_draw = ImageDraw.Draw(mask)
corner_r = int(SIZE * 0.22)  # Google Play uses ~22% corner radius
mask_draw.rounded_rectangle([0, 0, SIZE, SIZE], radius=corner_r, fill=255)

# Draw wifi icon centered
cx, cy = SIZE // 2, SIZE // 2
scale = SIZE / 108  # scale from 108dp viewport to 512px
draw_wifi_icon(draw, cx, cy, scale)

# Apply rounded mask
bg = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
icon = Image.composite(icon, bg, mask)
icon.save(os.path.join(OUT, "app-icon-512.png"), "PNG")
print(f"  -> {OUT}\\app-icon-512.png")


# ─── 2. Feature Graphic 1024×500 ────────────────────────────────────
print("Generating feature graphic 1024x500...")
FW, FH = 1024, 500
feat = gradient_bg(FW, FH)
draw = ImageDraw.Draw(feat)

# WiFi icon on the left-center
wifi_cx = FW // 4
wifi_cy = FH // 2
draw_wifi_icon(draw, wifi_cx, wifi_cy, scale=3.5)

# Text on the right
try:
    font_title = ImageFont.truetype("arial.ttf", 56)
    font_sub = ImageFont.truetype("arial.ttf", 24)
except Exception:
    font_title = ImageFont.load_default()
    font_sub = ImageFont.load_default()

text_x = FW // 2 + 20
draw.text((text_x, FH // 2 - 60), "Birdo VPN", fill=WHITE, font=font_title)
draw.text((text_x, FH // 2 + 10), "Fast & Secure WireGuard VPN", fill=WHITE_A, font=font_sub)
draw.text((text_x, FH // 2 + 45), "No logs. No ads. No trackers.", fill=WHITE_A, font=font_sub)

feat.save(os.path.join(OUT, "feature-graphic-1024x500.png"), "PNG")
print(f"  -> {OUT}\\feature-graphic-1024x500.png")

print("\nDone! Assets saved to store-assets/")
