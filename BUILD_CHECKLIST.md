# Build / review checklist

- [x] Minecraft dependency pinned to 1.21.11
- [x] Fabric remap Loom used for the obfuscated 1.21.11 release
- [x] Java 21 source/target
- [x] Fabric API 0.141.6+1.21.11
- [x] Normal cards restricted to 2–9
- [x] Ace represented by 1 and scored as 11/1
- [x] Invalid or missing custom names are ignored safely
- [x] Host/Viewer role stored per item frame locally
- [x] One dispenser can supply both Host and Viewer cards
- [x] Host/Viewer assignment keybinds are configurable in Controls
- [x] Targeted frame role can be cleared
- [x] HUD positions can be dragged in an editor
- [x] HUD positions persist across restarts
- [x] Configuration read/write failures cannot crash Minecraft
- [x] Glow item frames work through ItemFrameEntity
- [x] HUD hides when Minecraft HUD is hidden
- [ ] Full Gradle/Minecraft compilation in an environment with Maven/Gradle dependency access
- [ ] In-game test on Minecraft 1.21.11
