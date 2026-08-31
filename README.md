# Blackjack Item Frame Calculator — Minecraft 1.21.11

Fabric client-side mod for Minecraft 1.21.11.

## How it works

Cards are simply custom-named items with names `1` through `10`. `1` is the Ace. 10 is a normal card worth 10 points.

Host/Viewer is **not encoded in the paper name**. Instead, each item frame is assigned a role locally, so both Host and Viewer papers can come from the **same dispenser**.

### Keybinds

All keybinds appear under **Options → Controls → Key Binds → Blackjack Calculator** and can be changed there:

- **Toggle Target Frame Host/Viewer** — default `H`. Look at an item frame and press the key. First press assigns Host, second press assigns Viewer, then it alternates. The key can be changed in Minecraft's Key Binds.
- **Clear Target Frame Role** — default `K`. Look at an item frame and press the key to stop the frame from being counted.
- **Edit HUD Positions** — default `P`. Opens an editor where Host and Viewer totals can be dragged anywhere on screen.

HUD positions and frame roles are saved locally in `blackjackcalculator.json` in the Minecraft game directory.

### Blackjack scoring

- Normal cards: `2`–`10`.
- Ace: `1`, scored as 11 when possible and automatically reduced to 1 when 11 would cause a bust.
- Multiple Aces are handled correctly.

### One-dispenser setup

You can keep all of your papers in one dispenser. Put any card paper into any frame, then use the Host/Viewer keybind while looking at that frame to assign its role.

The same visible card name can be used on both sides. For example, a paper named `1` can be a Host Ace in one frame and a Viewer Ace in another frame.
