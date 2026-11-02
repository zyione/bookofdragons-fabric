# Book of Dragons - Player Guide

For the unofficial Fabric 1.20.1 development port. Updated 2 November 2026.
Start with section 1, then follow the sections you need. Server datapacks,
configuration changes and remapped controls can change your experience.

Species-by-species wiki:
https://github.com/zyione/bookofdragons-fabric/tree/codex/fabric-1.20.1/docs/dragon-wiki

## 01. Install the right files

Use **Minecraft Java 1.20.1**, **Java 17**, and **Fabric**.
The standalone setup was tested with Loader **0.18.2**; the tested
BigChadGuys Plus instance uses **0.19.5**. Keep a modpack's required newer
loader rather than downgrading it.

Close Minecraft and back up your world before installing or updating mods.
Get compiled jars from your pack/server organizer or the prepared release bundle.
Put these two normal gameplay jars in your instance's `mods` folder:

- `bookofdragons-fabric-1.4+1.20.1.fabric.dev.1.jar`
- `fauna-engine-fabric-1.0+1.20.1.fabric.dev.1.jar`

Required dependencies:

- **Fabric API 0.92.12+1.20.1**.
- **AzureLib 3.2.0 for Fabric 1.20.1**; tested version ID `VDXznzXQ`.
- **Forge Config API Port 8.0.1**; tested version ID `Qs8LaZLo`.
- **Mod Menu 7.2.2** is optional.

Forge Config API Port is a Fabric dependency. It does not mean you should
install Forge itself.

Use one copy of each required mod. Replace older copies when updating.
Keep Book of Dragons and FAUNA from the same current delivery. Do not mix in
their Forge versions or install `-sources.jar`, `-dev.jar`, or
`-verification.jar` files. The `.fabric.dev.1` inside the two filenames above
is part of this development release's name.

Start the game. If it reports missing dependencies, fix those before opening
a world. Existing Forge worlds have not been verified for migration.
