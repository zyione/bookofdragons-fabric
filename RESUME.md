# Book of Dragons Fabric port checkpoint

Launcher crash repair: explosion callback now coexists with Dragons of Berk;
the standalone null-entity ray fallback is skipped when Porting Library provides
the identical implementation. Ten GameTests and eight client checks passed in
both the minimal packaged runtime and the isolated launcher-mod compatibility
run (Loader 0.19.5). The real launcher instance was not modified.

The Fabric port implementation, source/build setup and automated verification
are complete for the development delivery. Both runtime jars are under
`bookofdragons/build/libs` and `fauna/build/libs`. See README.md for exact
dependencies, installation, provenance and save limitations.

User requested the port, a new directory under fabric-port, a goal, occasional
completion percentages, final tests, and push to
https://github.com/zyione/bookofdragons-fabric.git. Their later preference is
automated server tests and client harnesses instead of full desktop control.
The user paused then explicitly resumed the task.

Verification (2026-10-31): 3 JUnit tests, 10 GameTests, 8 packaged Java 17 client
checks including flight, breath controls and C2S dismount, and Java 17 dedicated
server spawning all nine species. Both release jar hashes are reproducible.
All 1085 retained reference resources also match their staged Git blobs.
See docs/verification-results.json and docs/VERIFICATION.md. Longer play,
all species/effect combinations, real-player latency and optional-mod coverage
remain explicitly unverified; do not claim exhaustive parity.

Use the fabric-1-20-1-port skill for further changes. Run Gradle with JDK21;
release classes target Java17. `gradlew.bat :bookofdragons:runGametest` uses
an isolated server; `:bookofdragons:runClientVerification` automatically creates
or reuses `.reference/dev-client/saves/Port Verification`. The packaged-client
scripts use `.reference/packaged-client` and their own test world. No launcher
instance or live server was modified. All test processes stopped after checks.

Reference binaries, recovered baseline trees, downloaded tools, isolated worlds
and full logs stay under excluded `.reference`. Never rerun the one-shot
prepare/recovery scripts over the edited native source. Vineflower1.12.0,
MCP1.20.1 and Mojang mappings were used. Original notices remain in each
module resource LICENSE. The sealed core is a required asset and is unchanged.

GitHub was authenticated via the existing Git credential helper: target repo
is PUBLIC, empty before this push, with push permission. No visibility change
was requested. Branch is codex/fabric-1.20.1. Review commit/push result and
`git status` if resuming during delivery.

Daily continuation heartbeat id: continue-book-of-dragons-port, 11 AM Manila.
It checks account usage before substantive work and defers at <=2% remaining.
On goal completion it should stay quiet; disable the heartbeat after successful
delivery. Last reliable usage query this resumed turn had 83% remaining.
A later usage query stalled and was cancelled; do not interpret that as zero.
