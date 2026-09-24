# Blackjack Item Frame Calculator — Minecraft 1.21.11

Fabric client-side mod for Minecraft 1.21.11.

## How it works

The mod reads map IDs from item frames and uses the locally scanned dispenser/dropper layout to assign blackjack values. Host and Viewer are assigned by selecting rectangular item-frame areas locally, so the same card/map can be used on either side.

## Keybinds

All keybinds appear under Options → Controls → Key Binds → Blackjack Calculator and can be changed there:

- Select Host Area (Item Frames) — default J. Look at one corner frame, press J, then look at the opposite corner and press J again.
- Select Viewer Area (Item Frames) — default U. Same two-corner selection process for the Viewer.
- Edit HUD Positions — default P. Opens the HUD editor.
- Scan Dispenser / Dropper — default K. While the dispenser/dropper screen is open and targeted, captures the nine card/map values and their local map visuals.
- Toggle map simulation — press R. R is polled directly and is not a configurable keybind.

## Blackjack scoring

- The scanned card values are assigned from the dispenser/dropper slots.
- Ace is represented by 11 and is automatically reduced to 1 when needed to avoid a bust.
- Hands are counted independently for Host and Viewer.
- Empty hands display -; totals above 21 display BUST.

## Map simulation

R toggles a client-only simulation mode. The current build does not use the incompatible legacy MapRenderer mixin that caused startup crashes on Minecraft 1.21.11.

- Item-frame map visuals in the selected Host/Viewer areas can use a simulated scanned map representation.
- Counting uses the fake value corresponding to the simulated map when one exists.
- If a fake value is unavailable, counting falls back to the real scanned value instead of stopping automatic counting.
- The simulation does not modify the server-authoritative ItemStack.
- The Viewer simulation is kept at 20 or below, while the Host simulation is forced above the Viewer when both sides have cards.
- Container-heart rendering is adjusted locally while simulation is enabled.

## Selection and persistence

HUD positions, display names, scanned map values, and frame selections are saved locally in blackjackcalculator.json in the Minecraft game directory.

The mod is client-side only; the server does not need the mod installed.
