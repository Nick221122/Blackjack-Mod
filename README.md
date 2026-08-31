# Blackjack Item Frame Calculator — Minecraft 1.21.11

Client-side Fabric mod for Minecraft Java 1.21.11.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.2+
- Fabric API 0.141.6+1.21.11
- Java 21+

## Features

- Blackjack scoring for numbered cards `1` through `10`.
- Ace (`1`) automatically scores as 11 when possible and is reduced to 1 when required to avoid a bust.
- Host and Viewer roles with completely independent saved state.
- Item-frame role assignment without changing or reinterpreting the card value.
- Persistent Host/Viewer names, frame assignments, scanned container assignments, HUD positions, and HUD scales.
- HUD score display plus active-side indicator.
- HUD position/size editor with reset support.
- Scan review screen.
- Dispenser/Dropper scanner that reads only the currently opened nine-slot container and never moves, consumes, or changes its items.

## Controls

All controls can be changed in **Options → Controls → Key Binds → Blackjack Calculator**.

| Default | Action |
|---|---|
| `H` | Switch active side between Host and Viewer |
| `J` | Assign the item frame under the crosshair to the active side |
| `K` | Clear the item frame role under the crosshair |
| `P` | Open the HUD position/size editor |
| `O` | Open the scan review screen |
| `N` | Open Host/Viewer naming configuration |

## Dispenser / Dropper scanning

Open a Dispenser or Dropper normally. A **Scan** button appears on the left side of the container GUI.

When pressed, the scanner:

1. Verifies that the currently opened block is a Dispenser or Dropper.
2. Reads the nine container slots from top-left to bottom-right.
3. Numbers occupied slots sequentially from `1` to `9`.
4. Reads the numbered card value from the item's custom name (`1`–`10`).
5. Saves the slot-to-value assignment under the currently active Host or Viewer side.
6. Leaves every inventory stack untouched.

A later Host/Viewer switch only changes the active side. It never swaps, moves, deletes, or reinterprets saved scans.

The active scan for each side is stored separately, while previously scanned container assignments remain in the local configuration file.

## Paper/card system

Numbered paper/card items are represented by their custom display name. A card named `1` is an Ace; names `2`–`10` are normal card values. The scanner accepts any item with a custom name exactly matching one of those values.

Item frames use their locally assigned Host/Viewer role, so the same numbered item can independently appear on either side.

## Persistent configuration

The mod stores client-only state in `blackjackcalculator.json` in the Minecraft game directory. Host and Viewer data use separate namespaces for scanned assignments and active containers.

## Build

The project is configured for the requested Fabric 1.21.11 versions and produces the archive name `blackjack-calculator-1.21.11.jar` when Gradle is run. The included `gradlew` and `gradlew.bat` bootstrap Gradle 8.14.3 when it is not already installed locally.
