# Port ledger

Reconstructed Book of Dragons 1.4 and required FAUNA Engine 1.0 for Fabric
1.20.1. This is not the authors' original source build. The native integration
is implemented and build-checked; verification levels are separated below.
Long-session gameplay and real-player latency coverage remain manual gaps.

| System | Reference trigger / result | Fabric implementation | Evidence |
| --- | --- | --- | --- |
| Recovery | 282 Book and 1461 FAUNA compiled classes | 208 and 964 baseline Java files, plus native adapters; no unresolved SRG tokens | Both modules compile; named and intermediary runtime checks |
| Registries | Forge deferred-register lifecycle | FabricRegistration and coordinated FabricBootstrap | All nine species finalized with attributes/genetics; packaged summon/tick checks |
| Loader/side separation | Forge entrypoints, common setup, client subscriptions | FaunaFabric, BookOfDragonsFabric, FabricClientBridge and client content entrypoint | Dedicated server and client load on Java 17 |
| Network | 30 FAUNA and 13 Book core messages, directional channel registration | FabricNetwork; exact protocol login queries; game-thread handlers; bounded fragmented C2S saves | 100-page save over actual transport; malformed decode and ownership/range tests; real-player latency pending |
| Common hooks | Forge server/world/player/entity/chunk lifecycle, damage/death/taming/advancement, tags, reload, explosion, block use/place/break | FabricCommonBridge plus CommonEntityEvents, CommonLivingEvents, TamingEvents, AdvancementEvents, ExplosionEvents and block mixins | Save, ownership, discovery, paralysis, egg and packet GameTests; every rare event path not individually triggered |
| Render hooks | Forge render stages, frame phases, entity renderers, particles, shaders, reloaders | FabricClientBridge, RenderStageEventsMixin and original renderer/animation code | Real client world/dragon/menu/flight/breath frames; every effect/variant still needs extended visual play |
| Input/camera/HUD/hands | Forge input contexts, shared bindings, arm poses, first-person transforms, HUD layers, FOV | Native client mixins and context-aware keybindings; custom poses; per-layer HUD replacement | Mounted and airborne camera/HUD, inventory/book, C2S attacks/dismount; all key combinations/preferences pending |
| Multipart | Forge PartEntity, global parts and entity-ID lookup | PartBody, PartIndex, LevelPartsMixin and server/client part lookup | GameTest lookup and collision query; client tracked hitboxes and rider scenes |
| Discovery | Forge capability attach/save/load/clone | PlayerDiscoveryMixin, DiscoveryCarrier, COPY_FROM callback | Serialization/copy GameTest and real book synchronization; long reconnect/death session pending |
| Persistent entity data | Forge getPersistentData and legacy load | EntityData and PersistentEntityDataMixin retaining ForgeData | All species data/owner save-reload test; PlayerPersisted clone handling |
| Paralysis | Forge effect eligibility/add/remove/expire, attacks, jumps, knockback, projectile spawn and input | ParalysisLivingMixin, ParalysisSpawnMixin, ParalysisFabric callbacks, ParalysisInputMixin | Immunity and AI restoration GameTest; input/attack variants need long play coverage |
| Gameplay | Nine species, FAUNA AI/genetics/movement/combat/ownership components | Shared recovered logic retained; native lifecycle services | All species initialization/ticking/save, genetics on client; actual takeoff/flight/breath controls |
| Eggs/equipment | Incubation, baby creation, parent lineage, item pickup, saddle/chest data | Shared logic and native menu transport | Hatch and genetic baby GameTest, egg/item and equipment round trips, rendered extended inventory |
| Config | ForgeConfigSpec TOML defaults and config screen | Tested Forge Config API Port 8.0.1; native registration; optional Mod Menu factory | Defaults loaded client/server; config-screen combinations not exhaustively exercised |
| Worldgen/roost/trades/data | Structure processor, roost block entities/modules, spawn engine, trades, data packs | Original shared code/data; native registration/trade sinks; lazy external genetics pack sources | Data load, registries, resource hashes, natural test-world generation; distribution and progression manual |
| Assets | Original models/textures/animations/audio/font/core resources | Original bytes retained; Forge generated mixin/refmap metadata replaced | 1002 Book and 77 FAUNA asset/data hashes identical; sealed core retained |
| Separate item models | Forge separate-transforms definitions for book, renamed book and rattlestaff | Fabric model-loading callback and render-context model selection | Packaged minimal and launcher-mod clients: textured quads in every context, distinct rename icon, hotbar and held-book screenshots |
| Packaging | Java 17 runtime and installation namespace | Loom remapJar; exact pinned dependencies; reproducible archives | Java 17 packaged server/client, both release hashes identical after full rerun |

## Differences and provenance

- Original author links did not publish a source repository in project metadata.
  User supplied the release jar and requested reconstruction/porting/push.
  Original code and asset notices are retained separately in module LICENSE files.
- Forge-specific entrypoints, event adapters and discovery capability are replaced
  by native Fabric counterparts. Forge data-generation classes remain reference
  material in excluded `.reference`; generated runtime data is preserved. This
  project builds from those resources rather than rerunning Forge datagen.
- Recovery fixes restore lost generic types, scope boundaries, a cast for
  RigLod's Entity overload, a reflective Vector3d cast, and mixin `(Object)this`
  casts. RigLod behavior was checked against original bytecode.
- Forge's nullable clip context is retained through a small constructor redirect.
- Native packet receivers reject oversized, non-finite and trailing payload data;
  multipart arrays are bounded before allocation. Reporter election and owner/
  rider checks stay in original gameplay handlers. Fragmentation preserves the
  book's 100-page note capacity beyond vanilla C2S's 32 KiB packet limit.
- Client pack providers are discovered lazily; eager discovery occurred before
  Fabric mod initialization and omitted the sealed genetics pack.
- HOBGOBBLER is an unregistered reference enum placeholder, not a missing tenth
  species. The port retains the placeholder and nine actual entity registrations.
- The chunk-ticket service has no callers in these supplied artifacts. Its
  native per-owner tickets are implemented; persistence across restart for this
  unused support API is unverified.

See VERIFICATION.md for known reference warnings and the explicit remaining
manual scenarios, source-inventory.json for the source-derived inventory, and
verification-results.json for artifact hashes and completed automated checks.
