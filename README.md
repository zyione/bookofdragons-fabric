# Book of Dragons — Fabric 1.20.1

Unofficial reconstruction of Book of Dragons 1.4 for Minecraft Java 1.20.1,
with its required FAUNA Engine 1.0 port. This repository is a development
port, not the authors' original source tree or an official release.

Original authors and notices are retained in each module's resource `LICENSE`.
The original code, models, textures, animations, sounds and data remain under
their original All Rights Reserved license; this repository does not grant
additional rights to that content.

## Installation

For players, see the sectioned [player guide](docs/PLAYER_GUIDE.md).
The [Discord posting instructions](docs/discord-guide/POSTING.md) explain how to
share its numbered messages with headings, controls and copyable commands.

Use Minecraft **1.20.1**, Java **17 or later**, and Fabric Loader **0.18.2**.
Install both output jars in the instance's `mods` directory:

- `bookofdragons-fabric-1.4+1.20.1.fabric.dev.1.jar`
- `fauna-engine-fabric-1.0+1.20.1.fabric.dev.1.jar`

Also install these exact tested dependencies:

| Dependency | Tested artifact |
| --- | --- |
| Fabric API | `0.92.12+1.20.1` |
| AzureLib | `azurelib-fabric-1.20.1-3.2.0.jar`, Modrinth version `VDXznzXQ` |
| Forge Config API Port | `8.0.1`, Modrinth version `Qs8LaZLo` |
| Mod Menu (optional config button) | `7.2.2`, Modrinth version `lEkperf6` |

Do not install the Forge versions of Book of Dragons or FAUNA alongside these
jars. Do not install the `-sources`, `-dev`, or `-verification` jars for normal
play. Tests use isolated worlds and are not deployed to launcher instances.

Back up saves before trying this development port. Registry IDs, dragon NBT,
and persistent `ForgeData` are retained where applicable. Discovery can read
the reference `ForgeCaps` discovery tag, but this does **not** establish full
Forge-world migration compatibility. Other Forge mods and worldgen can have
separate incompatibilities.

## Build

Run Gradle with **JDK 21**. The mod classes target **Java 17**; the build JVM
and game-runtime requirements are separate.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
.\gradlew.bat build
```

Output jars are in `bookofdragons/build/libs` and `fauna/build/libs`. Gradle
9.2.1, Loom 1.14.10 and all runtime dependencies are pinned. Loom performs
the required production remapping to intermediary names.

## Automated verification

```powershell
.\gradlew.bat :fauna:test
.\gradlew.bat :bookofdragons:runGametest
.\gradlew.bat :bookofdragons:runClientVerification
```

The client harness creates or reuses an isolated test world named
`Port Verification` under `.reference/dev-client/saves`, and takes
game-rendered screenshots without desktop control. It changes only that test
world and needs no manual world setup. It spawns all
nine species, opens the book and inventory, tests large note saves, mounts,
and exercises flight/dismounting. Verification sources and metadata are in
`bookofdragons/src/gametest`; they are excluded from release jars.

`tools/prepare_packaged_client.py`, `tools/run_packaged_client.py`, and
`tools/run_packaged_server.py` reproduce the isolated Windows Java 17
production-runtime checks. They require prepared build outputs; the server
runtime can be installed using Fabric Installer 1.1.2 with Minecraft 1.20.1
and Loader 0.18.2. Runtime downloads, worlds and logs stay in `.reference`.

See [the port ledger](docs/PORT_LEDGER.md) and
[verification evidence](docs/VERIFICATION.md) for precise results and gaps.
The one-shot source recovery scripts in `tools` document provenance; do not
run them over the edited implementation.
