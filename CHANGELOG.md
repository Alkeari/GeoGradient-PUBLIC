# Changelog

What changed in GeoGradient, newest first. Only changes you can actually notice are listed:
internal and build work is deliberately left out. This file is the source for the release
notes published on the CurseForge and Modrinth pages.

The 1.20.1 line's releases 1.0.0-1.20.1 and 1.0.1-1.20.1 predate this file; their notes exist
only on the published pages, since this repository's earlier history was intentionally cleared.

From v2.0.0 one release covers every supported Minecraft version and loader, so this file is the only changelog.

## v2.0.0 - 2026-10-14

- Added: One jar per loader, versioned 2.0.0, covers every supported Minecraft version and loads the build that matches yours.
- Added: Nexus Mods also offers a single jar that runs on Fabric, Forge and NeoForge alike.
- Added: Support for Minecraft 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6 and 1.21 on Fabric, 1.20.2, 1.20.3, 1.20.4, 1.20.6 and 1.21 on Forge, and 1.20.4, 1.20.5, 1.20.6 and 1.21 on NeoForge.
- Changed: The climate bands now sit at Earth's climate latitudes and were measured against vanilla's biome mix, so hot land covers 40% of the world instead of 64% and every vanilla surface biome generates, including Taiga, Flower Forest and Sunflower Plains.
- Added: Every band's latitude, temperature and humidity can be set, along with new `temperature_variation` and `humidity_variation` settings that mix neighboring biomes into each band.
- Added: In-game settings screens, through Mod Menu on Fabric and the Mods screen on NeoForge 1.21 and 1.21.1, with a tooltip on every setting.
- Added: Settings to turn off the F3 debug line, TerraBlender's region lookup and the `/geogradient info` command, each on by default.
- Added: Humid biomes such as Dark Forest and VanillaBackport's Pale Garden now generate in temperate land.
- Fixed: Biomes that mods add through Biolith, such as Promenade's and Let's Do BloomingNature's, now generate alongside GeoGradient.
- Fixed: Climate now changes smoothly across every band border instead of snapping back to the warmer band just past it, so newly generated land near a border can differ from before.
- Changed: The Forge build accepts Forge 47.1 and later, so it also runs on the NeoForge 1.20.1 fork.
- Removed: GeoGradient no longer requires Architectury API.

## v1.0.4-1.21.1 - 2026-10-14

- Changed: New mod-list icon, the same artwork as the store page tile.

## v1.0.3-1.21.1 - 2026-09-06

- Fixed: The Forge and NeoForge mod list descriptions still showed a long dash; they now use the plain hyphen the previous release said they did.

## v1.0.2-1.21.1 - 2026-09-04

- Changed: The not-activated warning uses a plain hyphen instead of a long dash.

## v1.0.1-1.21.1 - 2026-09-01

- Added: Minecraft 1.21.1 support on Fabric and NeoForge, verified to run on 1.21.1 through 1.21.11.
