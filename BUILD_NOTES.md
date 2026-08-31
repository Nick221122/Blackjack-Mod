# Minecraft 1.21.11 build notes

This project targets Minecraft 1.21.11, Fabric Loader 0.19.2+, Fabric API 0.141.6+1.21.11, and Java 21.

Minecraft 1.21.11 is obfuscated, so the project uses `net.fabricmc.fabric-loom-remap` as required by Fabric Loom documentation.

Build with the Gradle wrapper from the repository root:

- Windows: `gradlew.bat build`
- macOS/Linux: `./gradlew build`

The production remapped JAR is written to `build/libs/`.
