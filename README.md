# Blackjack Item Frame Calculator — Minecraft 1.21.11

Fabric client-side mod for Minecraft 1.21.11.

## How it works

The mod scans card maps/papers in a dispenser or dropper and nearby item frames automatically. Card names are `1` through `10`; `1` is the Ace and `10` is worth 10 points. Host and Viewer totals are calculated separately from the frame layout.

## Keybinds

All keybinds appear under **Options → Controls → Key Binds → Blackjack Calculator** and can be changed there:

- **Scan Container** — default `L`. Open a dispenser/dropper and press this to scan its 9 slots and the nearby item frames. The frame area on the left is Viewer; the area on the right is Host, with up to 9 frames per side.
- **Edit HUD Positions** — default `P`. Opens the editor where Host and Viewer totals can be dragged anywhere on screen.
- **Toggle Map Simulation** — default `R`. Toggles the map simulation feature.

There are no manual Host/Viewer frame-selection keybinds. Frame detection is handled by the scanner.

HUD positions and other local settings are saved in `blackjackcalculator.json` in the Minecraft game directory.

## Blackjack scoring

- Normal cards: `2`–`10`.
- Ace: `1`, scored as 11 when possible and automatically reduced to 1 when 11 would cause a bust.
- Multiple Aces are handled correctly.

## Build download

Open the [GitHub Actions build workflow](https://github.com/Nick221122/Blackjack-Mod/actions/workflows/build.yml?query=branch%3Adownload%2Fhost-viewer-scan-keybind), select the newest successful run for this branch, and download the `blackjack-calculator-1.21.11` artifact. Extract the ZIP to get the mod JAR.
