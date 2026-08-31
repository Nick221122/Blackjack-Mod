# Minecraft 1.21.11 runtime checklist

- [x] Minecraft dependency pinned to 1.21.11
- [x] Remapping Loom plugin used for obfuscated 1.21.11
- [x] Java 21
- [x] Fabric API 0.141.6+1.21.11
- [x] Fabric Loader 0.19.3 (satisfies 0.19.2+)
- [x] Client-side entrypoint
- [x] Host/Viewer automatic calculation
- [x] Persistent frame roles
- [x] Configurable keybinds
- [x] Ace 1/11 scoring and BUST
- [x] 1-10 card values
- [x] Item-frame scanning
- [ ] Final GitHub Actions production build must pass before treating the JAR as release-ready
- [ ] In-game smoke test on a real 1.21.11 Fabric client

Do not install the project as a final release until the last two checks pass.
