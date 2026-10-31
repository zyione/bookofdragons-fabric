# Verification evidence

The automated checks exercise the native Fabric implementation. They are not
a claim of exhaustive behavior parity or a comparison with a live Forge game.

## Passed

- Java 17 bytecode in both remapped installation jars.
- Development compilation and packaging with JDK 21, Gradle 9.2.1, Loom 1.14.10.
- Three JUnit tests: independent-player large transfers, malformed/replayed
  fragments, disconnect and timeout cleanup.
- Eleven Fabric GameTests: every actual dragon species finalizes with genetics,
  ticks, and round-trips ownership/data; multipart lookup/collision queries;
  sound-listener tags; discovery/notes serialization and copying; paralysis,
  immunity and AI restoration; non-owner and distance authorization;
  equipment and egg persistence; malformed controls; actual egg hatching;
  mutable explosion entity filtering before damage; null-entity ray queries;
  nearest-entity ownership commands from a position closer to a hitbox than the
  parent, followed by ticking, saddling and a successful mounting permission check.
- Development dedicated server readiness; isolated packaged Java 17 server
  readiness and graceful save/shutdown.
- Development and packaged Java 17 clients: genetic appearance synchronization,
  rendered world frames, book, extended inventory, mounted camera/HUD, and
  a real fragmented 100-page C2S note save.
- Packaged Java 17 client: adult takeoff, synchronized flight, rider-camera
  rendering, and C2S dismount with both client and server detached.
- Configured Night Fury breath charge/release through the actual C2S controls
  while mounted; charge and release frames rendered without a crash.
- Packaged dedicated server: summoned and ticked all nine species before
  gracefully saving and stopping.
- Rebuilt both release jars with all tasks rerun; both SHA-256 hashes matched.
- Repaired jars in an isolated copy of the user's launcher-pack mods, using
  Fabric Loader 0.19.5 and Java 17: 83 top-level jars including the test harness,
  Dragons of Berk, Create/Porting Library, Lithium and ModernFix. All nine client
  checks passed and the process exited cleanly. User configs, resource packs,
  shaders and existing saves were not copied, so those remain outside this check.
- Asset/data hashes: 1,002 Book resources and 77 FAUNA resources match their
  reference jars, with none removed, added or changed in the audit's asset/data
  scope. The sealed `bookofdragons.core` is retained as a required resource.

Reports are generated under `.reference`. Release-only tests are never bundled
into normal installation jars. The exact artifact hashes and final scenario
results are summarized in `verification-results.json` when generated.

## Remaining manual coverage

These scenarios need longer play sessions or multiple real players:

- Full courtship/taming/QTE progression for each species, breeding and all
  habitat/roost conditions over natural time.
- Every attack, visual effect, variant, named skin, sound, toy interaction,
  flight maneuver and camera preference across all species.
- Natural-world distribution, structure exploration and villager progression.
- Real-player latency, simultaneous riders, reconnects, dimension transfers,
  death/respawn, and compatibility with optional mods such as Sable or JourneyMap.

## Known reference warnings

The supplied reference has duplicate `crystal` and `vortex` keys in two genetics
JSON files; the bytes are preserved. Legacy particle descriptors reference
sprites absent from the supplied archive/core. Some animation expressions
produce AzureLib parsing warnings, and instancing shaders have unused uniforms.
These warnings are recorded, rather than fixed by inventing replacement art
or changing reference gameplay. Actual effects use FAUNA's newer attack
definitions; the old descriptors still require full visual-play coverage.

Vanilla logs missing DFU registrations for custom entity types. Optional Sable
mixin targets are absent without Sable. Offline test accounts cannot access
Realms; test servers bind only to loopback.

## Port-specific issues found and corrected

- Exclude FAUNA's nonpersistent multipart hitboxes from command selectors before
  sorting and limiting. Physical queries still include them. The new regression
  test failed on the previous jar because the nearest Night Fury was a hitbox,
  then passed with the selector fix and both ownership commands applied to the
  dragon. Hitboxes have their parent's entity type but cannot save taming fields.

- Supply Fabric model baking and display-context selection for the reference
  `forge:separate_transforms` book, renamed book and rattlestaff definitions.
  Their original textures, 2D/3D geometry and rename predicate remain intact.
  Packaged client checks verify textured geometry for every display context and
  a distinct renamed-book icon. Screenshots confirm hotbar icons and the held
  3D book in both minimal and launcher-mod compatibility runtimes.

- Launcher-pack compatibility repair: FAUNA now filters the collected explosion
  entity list instead of redirecting the same query as Dragons of Berk.
  The callback still runs before damage; a regression test verifies one callback,
  protected-entity exclusion and damage to an unfiltered entity.
- When `porting_lib_base` is loaded, use its identical null-entity collision-context
  implementation instead of applying a competing redirect. The standalone
  fallback remains required and covered by an entity-independent ray-query test.

- Wait for both mod initializers before registry/setup; otherwise FAUNA's
  call game event was missing from its listener tag.
- Discover client genetics packs lazily, because the client pack repository
  exists before Fabric initialization.
- Recover the `RigLod.of(FaunaBase)` cast to the `Entity` overload; original
  bytecode confirmed the decompiler had lost it, causing recursive rendering.
- Preserve Forge's null-entity collision context for flight wake ray queries.
- Provide the verification jar's full mod classpath during remapping so
  inherited game methods use the correct production names.
