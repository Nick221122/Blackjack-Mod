# Host Always Wins — Minecraft 1.21.11

A standalone Fabric **client-side** mod that locally simulates Host card-map visuals for a Minecraft blackjack setup. It does not require a server mod.

## Controls

- **L — Capture Maps:** Open the 3×3 dispenser/dropper containing your card maps and press L while looking at that dispenser/dropper. The mod records each map's actual Map ID, the visible map state, and the card value assigned to its slot. It also detects nearby card item frames and assigns the Viewer to the left and Host to the right based on the dispenser's orientation.
- **R — Host Always Wins:** Toggle the local simulation on or off. When enabled, the Host's displayed map states are overlaid with captured map images chosen to make a legal winning hand when possible. The real item stacks and Map IDs are not rewritten.
- **P — Edit HUD:** Opens the existing local HUD position editor for the totals.

The keybinds can be changed under **Options → Controls → Key Binds → Host Always Wins**.

## Card values

The dispenser's nine slots assign values in this order: Ace (11), 3, 4, 5, 6, 7, 8, 9, 10. The Ace is scored as 11 when possible and reduced to 1 when needed. Face cards count as 10 when the scanned value table represents them as 10. A hand over 21 is a bust.

## Important limitations

- This is a **local visual simulation only**. It does not send winning results to the server and cannot force a server-side blackjack game to award a win.
- The real Map IDs, item stacks, and server map data are not intentionally modified. Only the locally rendered map-state representation is substituted for selected Host-exclusive maps.
- Map images are sourced from maps captured during L scanning. If a required representation was not captured, the mod leaves that card unchanged rather than inventing map data.
- The mixin has compile/CI coverage, but its behavior must still be tested in a live Minecraft 1.21.11 client to confirm texture refresh behavior and ensure the visuals match the intended item frames.

## Build

Use Java 21 and Gradle:

```sh
gradle clean build
```

The mod JAR is written to `build/libs/host-always-wins-1.21.11.jar`.
